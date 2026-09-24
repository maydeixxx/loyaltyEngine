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
import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import com.LoyaltyEngine.WalletService.models.events.TransactionHandledEvent;
import com.LoyaltyEngine.WalletService.services.interfaces.*;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper mapper;

    @Value("${kafka.topics.transaction-handled}")
    private String transactionHandled;
    @Value("${kafka.topics.points-failed}")
    private String pointsFailed;

    private final MeterRegistry registry;
    private final Tracer tracer;

    public void createWallet(UUID userId) {
        try {
            boolean isWalletPresent = walletRepository.findWalletByUserId(userId).isPresent();
            if (isWalletPresent) {
                throw new WalletExistsException("Wallet for user [%s] already created".formatted(userId));
            }
            walletRepository.save(walletMapper.domainToEntity(WalletDomain.createWallet(userId)));
        } catch (WalletExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating wallet for user {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void creditPoints(UUID userId, UUID transactionId, BigDecimal amount, Boolean useCashback, BigDecimal amountOfTransaction, BigDecimal totalItemPrice) throws JsonProcessingException {
        ScopedSpan span = tracer.startScopedSpan("wallet-credit-points-span");
        Timer.Sample timer = Timer.start(registry);

        try {
            WalletDomain wallet = findWalletByUserId(userId);
            Optional<WalletTransaction> walletTransactionByTransactionId = walletTransactionRepository.findWalletTransactionByTransactionId(transactionId);

            if (walletTransactionByTransactionId.isPresent()) {
                log.warn("Transaction is already processed {}", transactionId);
                return;
            }

            if (wallet.getStatus() == WalletStatus.BLOCKED) {
                throw new WalletBlockedException(String.format("Wallet %s is blocked.", wallet.getId()));
            }

            Money balance = wallet.getBalance();
            if (useCashback && balance.isGreaterThan(Money.zeroOf())) {
                if (amountOfTransaction.compareTo(totalItemPrice) > 0) {
                    throw new IllegalArgumentException("Amount of transaction cant be greater than total item price");
                }

                Money cashbackToUse = new Money(totalItemPrice.subtract(amountOfTransaction));

                if (balance.isLessThan(cashbackToUse)) {
                    throw new InsufficientFundsException("Insufficient funds");
                }

                LocalDateTime redeemTimeStamp = LocalDateTime.now();
                wallet.debit(cashbackToUse.amount());

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

            if (!useCashback) {
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
            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(transactionId)
                    .eventType(transactionHandled)
                    .payload(mapper.writeValueAsString(transactionHandledEvent))
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.NEW)
                    .build();

            registry.counter("wallet.credit.points.counter", "status", "successful").increment();
            span.tag("status", "SUCCESSFUL");
            outboxEventRepository.save(event);
        } catch (IllegalArgumentException | WalletNotFoundException e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            UUID aggId = transactionId != null ? transactionId : (userId != null ? userId : UuidCreator.getTimeOrderedEpoch());
            PointsFailedEvent pointsFailedEvent = new PointsFailedEvent(
                    transactionId,
                    userId,
                    amount,
                    "Wallet for %s not found".formatted(userId),
                    LocalDateTime.now()
            );

            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(aggId)
                    .eventType(pointsFailed)
                    .payload(mapper.writeValueAsString(pointsFailedEvent))
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.NEW)
                    .build();

            outboxEventRepository.save(event);
        } catch (WalletBlockedException e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            UUID aggId = transactionId != null ? transactionId : (userId != null ? userId : UuidCreator.getTimeOrderedEpoch());
            PointsFailedEvent pointsFailedEvent = new PointsFailedEvent(
                    transactionId,
                    userId,
                    amount,
                    "Wallet is blocked",
                    LocalDateTime.now()
            );

            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(aggId)
                    .eventType(pointsFailed)
                    .payload(mapper.writeValueAsString(pointsFailedEvent))
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.NEW)
                    .build();

            outboxEventRepository.save(event);
        } catch (InsufficientFundsException e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            UUID aggId = transactionId != null ? transactionId : (userId != null ? userId : UuidCreator.getTimeOrderedEpoch());
            PointsFailedEvent pointsFailedEvent = new PointsFailedEvent(
                    transactionId,
                    userId,
                    amount,
                    "Insufficient funds",
                    LocalDateTime.now()
            );

            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(aggId)
                    .eventType(pointsFailed)
                    .payload(mapper.writeValueAsString(pointsFailedEvent))
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.NEW)
                    .build();

            outboxEventRepository.save(event);
        } catch (Exception e) {
            registry.counter("wallet.credit.points.counter", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error("Error crediting points to user {}: {}", userId, e.getMessage());
            UUID aggId = transactionId != null ? transactionId : (userId != null ? userId : UuidCreator.getTimeOrderedEpoch());
            PointsFailedEvent pointsFailedEvent = new PointsFailedEvent(
                    transactionId,
                    userId,
                    amount,
                    "Unknown error",
                    LocalDateTime.now()
            );

            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .aggregateId(aggId)
                    .eventType(pointsFailed)
                    .payload(mapper.writeValueAsString(pointsFailedEvent))
                    .retryCount(0)
                    .createdAt(LocalDateTime.now())
                    .status(OutboxStatus.NEW)
                    .build();

            outboxEventRepository.save(event);
        } finally {
            span.end();
            timer.stop(registry.timer("wallet.credit.points.duration"));
        }
    }

    @Transactional
    public void blockWallet(UUID userId) {
        try {
            WalletDomain wallet = findWalletByUserId(userId);
            wallet.blockWallet();

            walletRepository.save(walletMapper.domainToEntity(wallet));
            log.info("Wallet {} blocked", wallet.getId().value());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error blocking wallet: {}", e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Transactional
    public void unblockWallet(UUID userId) {
        try {
            WalletDomain wallet = findWalletByUserId(userId);
            wallet.activateWallet();

            walletRepository.save(walletMapper.domainToEntity(wallet));
            log.info("Wallet {} unblocked", wallet.getId().value());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error activating wallet: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public BigDecimal getBalance(UUID userId) {
        try {
            return findWalletByUserId(userId).getBalance().amount();
        } catch (Exception e) {
            log.error("Error getting balance for user {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<WalletTransactionDomain> getTransactionsHistory(UUID userId) {
        try {
            UUID walletId = findWalletByUserId(userId).getId().value();

            return walletTransactionRepository.getWalletTransactionsByWalletId(walletId)
                    .stream()
                    .map(walletTransactionMapper::entityToDomain)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting transactions for user {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public WalletDomain findWalletByUserId(UUID userId) {
        try {
            return walletMapper.entityToDomain(walletRepository
                    .findWalletByUserId(userId)
                    .orElseThrow(() -> new WalletNotFoundException(String.format("Wallet by user id %s not found", userId))));
        } catch (WalletNotFoundException e) {
            log.error("Wallet not found");
            throw e;
        } catch (Exception e) {
            log.error("Unexcepted error: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
