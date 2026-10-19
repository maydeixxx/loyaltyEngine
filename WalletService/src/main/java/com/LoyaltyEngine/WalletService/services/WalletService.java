package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.*;
import com.LoyaltyEngine.WalletService.models.entity.OutboxEvent;
import com.LoyaltyEngine.WalletService.models.entity.WalletTransaction;
import com.LoyaltyEngine.WalletService.models.enums.OutboxStatus;
import com.LoyaltyEngine.WalletService.models.enums.TransactionType;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.Money;
import com.LoyaltyEngine.WalletService.models.events.CancelTransactionEventModel;
import com.LoyaltyEngine.WalletService.models.events.CancellationFailedEvent;
import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import com.LoyaltyEngine.WalletService.models.events.TransactionHandledEvent;
import com.LoyaltyEngine.WalletService.services.interfaces.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.uuid.UuidCreator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    private final OutboxEventService outboxEventService;
    private final ObjectMapper mapper;

    @Value("${kafka.topics.transaction-handled}")
    private String transactionHandled;
    @Value("${kafka.topics.points-failed}")
    private String pointsFailed;
    @Value("${kafka.topics.transaction-cancellation-failed}")
    private String cancellationFailed;
    @Value("${kafka.topics.transaction-cancellation-handled}")
    private String cancellationHandled;

    private final MeterRegistry registry;
    private final Tracer tracer;

    public void createWallet(UUID userId) {
        boolean isWalletPresent = walletRepository.findWalletByUserId(userId).isPresent();
        if (isWalletPresent) {
            throw new WalletExistsException("Wallet for user [%s] already created".formatted(userId));
        }
        walletRepository.save(walletMapper.domainToEntity(WalletDomain.createWallet(userId)));
    }

    @Transactional
    public void creditPoints(UUID userId, UUID transactionId, BigDecimal amount, Boolean useCashback, BigDecimal amountOfTransaction, BigDecimal totalItemPrice) {
        ScopedSpan span = tracer.startScopedSpan("wallet-credit-points-span");
        Timer.Sample timer = Timer.start(registry);

        try {
            if (amountOfTransaction.compareTo(totalItemPrice) > 0) {
                throw new IllegalArgumentException("Amount of transaction cant be greater than total item price");
            }

            WalletDomain wallet = findWalletByUserId(userId);
            List<WalletTransaction> walletTransactions = walletTransactionRepository.findWalletTransactionByTransactionId(transactionId);

            if (!walletTransactions.isEmpty()) {
                log.warn("Transaction is already processed {}", transactionId);
                return;
            }

            if (wallet.getStatus() == WalletStatus.BLOCKED) {
                throw new WalletBlockedException(String.format("Wallet %s is blocked.", wallet.getId()));
            }

            Money balance = wallet.getBalance();

            Money cashbackToUse = new Money(totalItemPrice.subtract(amountOfTransaction));
            if (useCashback && balance.isGreaterThan(Money.zeroOf()) && !cashbackToUse.isZero()) {
                if (balance.isLessThan(cashbackToUse)) {
                    throw new InsufficientFundsException("Insufficient funds");
                }

                LocalDateTime redeemTimeStamp = LocalDateTime.now();
                wallet.debit(cashbackToUse.amount(), TransactionType.DEBIT);

                WalletTransactionDomain redeemCashback = WalletTransactionDomain.createWalletTransaction(
                        wallet.getId().value(),
                        transactionId,
                        cashbackToUse.amount(),
                        TransactionType.DEBIT,
                        redeemTimeStamp,
                        "Redeem cashback"
                );

                walletTransactionRepository.save(walletTransactionMapper.domainToEntity(redeemCashback));
                walletRepository.save(walletMapper.domainToEntity(wallet));
                span.tag("wallet-transaction-id", redeemCashback.getId().value().toString());

            } else if (amountOfTransaction.compareTo(totalItemPrice) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }

            if (!useCashback || cashbackToUse.isZero()) {
                LocalDateTime timestamp = LocalDateTime.now();
                wallet.credit(amount);

                WalletTransactionDomain walletTransaction = WalletTransactionDomain.createWalletTransaction(
                        wallet.getId().value(),
                        transactionId,
                        amount,
                        TransactionType.CREDIT,
                        timestamp,
                        "Cashback from transaction " + transactionId
                );

                walletTransactionRepository.save(walletTransactionMapper.domainToEntity(walletTransaction));
                walletRepository.save(walletMapper.domainToEntity(wallet));
                span.tag("wallet-transaction-id", walletTransaction.getId().value().toString());
                log.info("Points credited: user {} || transaction {} || amount of transaction {}", userId, transactionId, amountOfTransaction);
            }

            TransactionHandledEvent transactionHandledEvent = new TransactionHandledEvent(transactionId, userId);
            OutboxEvent event = buildOutboxEvent(transactionId, transactionHandled, transactionHandledEvent);
            registry.counter("wallet.credit.points.counter", "status", "successful").increment();
            span.tag("status", "SUCCESSFUL");
            outboxEventService.saveNewOutboxEvent(event);
        } catch (IllegalArgumentException | WalletNotFoundException e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            PointsFailedEvent pointsFailedEvent = buildPointsFailedEvent(transactionId, userId, amount, "Wallet for %s not found".formatted(userId));
            OutboxEvent event = buildOutboxEvent(transactionId, pointsFailed, pointsFailedEvent);
            outboxEventService.saveNewOutboxEvent(event);
        } catch (WalletBlockedException e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            PointsFailedEvent pointsFailedEvent = buildPointsFailedEvent(transactionId, userId, amount, "Wallet is blocked");
            OutboxEvent event = buildOutboxEvent(transactionId, pointsFailed, pointsFailedEvent);
            outboxEventService.saveNewOutboxEvent(event);
        } catch (InsufficientFundsException e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            PointsFailedEvent pointsFailedEvent = buildPointsFailedEvent(transactionId, userId, amount, "Insufficient funds");
            OutboxEvent event = buildOutboxEvent(transactionId, pointsFailed, pointsFailedEvent);
            outboxEventService.saveNewOutboxEvent(event);
        } catch (Exception e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error("Error crediting points to user {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        } finally {
            span.end();
            timer.stop(registry.timer("wallet.credit.points.duration"));
        }
    }

    @Transactional
    public void cancelTransaction(UUID transactionId, CancelTransactionEventModel transactionModel) {
        try {
            WalletDomain wallet = findWalletByUserId(transactionModel.userId());
            if (wallet.getStatus().equals(WalletStatus.BLOCKED))
                throw new WalletBlockedException("Cant cancel transaction, wallet is blocked");

            List<WalletTransactionDomain> walletTransactions = walletTransactionRepository.findWalletTransactionByTransactionId(transactionId)
                    .stream()
                    .map(walletTransactionMapper::entityToDomain)
                    .toList();

            for (WalletTransactionDomain walletTransaction : walletTransactions) {
                if (walletTransaction.getType().equals(TransactionType.CANCEL)) {
                    log.warn("Transaction already cancelled");
                    return;
                }
            }

            if (walletTransactions.isEmpty()) {
                throw new WalletTransactionNotFound("Cant find wallet transaction [%s]".formatted(transactionId));
            }

            for (WalletTransactionDomain walletTransaction : walletTransactions) {
                BigDecimal oldTransactionAmount = walletTransaction.getAmount().amount();
                if (walletTransaction.getType().equals(TransactionType.DEBIT)) {
                    wallet.credit(oldTransactionAmount);
                } else if (walletTransaction.getType().equals(TransactionType.CREDIT)) {
                    wallet.debit(oldTransactionAmount, TransactionType.CANCEL);
                } else {
                    throw new IllegalArgumentException("Unknown type of transaction: " + walletTransaction.getType());
                }

                WalletTransactionDomain refundedWalletTransaction = WalletTransactionDomain.createWalletTransaction(
                        wallet.getId().value(),
                        transactionId,
                        oldTransactionAmount,
                        TransactionType.CANCEL,
                        LocalDateTime.now(),
                        "Cancellation transaction - %s".formatted(transactionId)
                );
                walletTransactionRepository.save(walletTransactionMapper.domainToEntity(refundedWalletTransaction));
                walletRepository.save(walletMapper.domainToEntity(wallet));
            }

            TransactionHandledEvent transactionHandledEvent = new TransactionHandledEvent(transactionId, transactionModel.userId());
            OutboxEvent outboxEvent = buildOutboxEvent(transactionId, cancellationHandled, transactionHandledEvent);
            outboxEventService.saveNewOutboxEvent(outboxEvent);
        } catch (WalletNotFoundException e) {
            CancellationFailedEvent failedEvent = new CancellationFailedEvent(
                    transactionId,
                    transactionModel.userId(),
                    "Wallet for user - [%s] not found".formatted(transactionModel.userId()),
                    LocalDateTime.now()
            );

            OutboxEvent outboxEvent = buildOutboxEvent(transactionId, cancellationFailed, failedEvent);
            outboxEventService.saveNewOutboxEvent(outboxEvent);
        } catch (WalletTransactionNotFound e) {
            CancellationFailedEvent failedEvent = new CancellationFailedEvent(
                    transactionId,
                    transactionModel.userId(),
                    "Wallet transaction - [%s] not found".formatted(transactionId),
                    LocalDateTime.now()
            );

            OutboxEvent outboxEvent = buildOutboxEvent(transactionId, cancellationFailed, failedEvent);
            outboxEventService.saveNewOutboxEvent(outboxEvent);
        } catch (WalletBlockedException e) {
            CancellationFailedEvent failedEvent = new CancellationFailedEvent(
                    transactionId,
                    transactionModel.userId(),
                    "Wallet for user - [%s] blocked".formatted(transactionModel.userId()),
                    LocalDateTime.now()
            );

            OutboxEvent outboxEvent = buildOutboxEvent(transactionId, cancellationFailed, failedEvent);
            outboxEventService.saveNewOutboxEvent(outboxEvent);
        } catch (Exception e) {
            CancellationFailedEvent failedEvent = new CancellationFailedEvent(
                    transactionId,
                    transactionModel.userId(),
                    "Unexpected error cancelling transaction for user - [%s]".formatted(transactionModel.userId()),
                    LocalDateTime.now()
            );

            OutboxEvent outboxEvent = buildOutboxEvent(transactionId, cancellationFailed, failedEvent);
            outboxEventService.saveNewOutboxEvent(outboxEvent);

            log.error("Unexpected error cancellation transaction [{}]: {}", transactionId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void blockWallet(UUID userId) {
        WalletDomain wallet = findWalletByUserId(userId);
        wallet.blockWallet();

        walletRepository.save(walletMapper.domainToEntity(wallet));
        log.info("Wallet {} blocked", wallet.getId().value());
    }

    @Transactional
    public void unblockWallet(UUID userId) {
        WalletDomain wallet = findWalletByUserId(userId);
        wallet.activateWallet();

        walletRepository.save(walletMapper.domainToEntity(wallet));
        log.info("Wallet {} unblocked", wallet.getId().value());
    }

    public BigDecimal getBalance(UUID userId) {
        return findWalletByUserId(userId).getBalance().amount();
    }

    public List<WalletTransactionDomain> getTransactionsHistory(UUID userId) {
        UUID walletId = findWalletByUserId(userId).getId().value();

        return walletTransactionRepository.getWalletTransactionsByWalletId(walletId)
                .stream()
                .map(walletTransactionMapper::entityToDomain)
                .toList();
    }

    public WalletDomain findWalletByUserId(UUID userId) {
        return walletMapper.entityToDomain(walletRepository
                .findWalletByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(String.format("Wallet by user id %s not found", userId))));
    }

    private OutboxEvent buildOutboxEvent(UUID aggregateId, String eventType, Object payload) {
        try {
            String stringPayload = mapper.writeValueAsString(payload);
            return OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(stringPayload)
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.NEW)
                    .retryCount(0)
                    .build();
        } catch (Exception e) {
            log.error("Error building outbox event: ", e);
            throw new RuntimeException(e);
        }
    }

    private PointsFailedEvent buildPointsFailedEvent(UUID transactionId, UUID userId, BigDecimal amount, String cause) {
        return new PointsFailedEvent(
                transactionId,
                userId,
                amount,
                cause,
                LocalDateTime.now()
        );
    }
}
