package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.exceptions.WalletExistsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletNotFoundException;
import com.LoyaltyEngine.WalletService.models.domain.enums.TransactionType;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.domain.enums.WalletStatus;
import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.domain.valueObjects.Money;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void creditPoints(UUID userId, UUID transactionId, BigDecimal amount, Boolean useCashback, BigDecimal amountOfTransaction, BigDecimal totalItemPrice) {
        try {
            WalletDomain wallet = findWalletByUserId(userId);

            if (wallet.getStatus() == WalletStatus.BLOCKED) {
                throw new WalletBlockedException(String.format("Wallet %s is blocked.", wallet.getId()));
            }

            Money balance = wallet.getBalance();
            if (useCashback && balance.isGreaterThan(Money.zeroOf())) {
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
                log.info("Points credited: user {} || transaction {} || amount of transaction {}", userId, transactionId, amountOfTransaction);
            }
        } catch (InsufficientFundsException | WalletBlockedException | WalletNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error crediting points to user {}: {}", userId, e.getMessage());
            throw new RuntimeException(e);
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
