package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.InsufficientFundsException;
import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.exceptions.WalletNotFoundException;
import com.LoyaltyEngine.WalletService.models.domain.TransactionType;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.domain.WalletStatus;
import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import com.LoyaltyEngine.WalletService.models.entity.WalletTransaction;
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

    private Wallet createWallet(Long userId) {
        return walletRepository.save(walletMapper.domainToEntity(WalletDomain.createWallet(userId)));
    }

    @Transactional
    public void creditPoints(Long userId, UUID transactionId, BigDecimal amount, Boolean useCashback, BigDecimal amountOfTransaction, BigDecimal totalItemPrice) {
        Wallet wallet = walletRepository
                .findWalletByUserId(userId)
                .orElseGet(() -> createWallet(userId));

        if (wallet.getStatus() == WalletStatus.BLOCKED) {
            throw new WalletBlockedException(String.format("Wallet %s blocked.", wallet.getId()));
        }

        if (useCashback) {
            BigDecimal cashbackToUse = totalItemPrice.subtract(amountOfTransaction);
            BigDecimal balance = wallet.getBalance();

            BigDecimal actualCashback;
            if (balance.compareTo(cashbackToUse) < 0) {
                BigDecimal itemsPriceAfterCashback = totalItemPrice.subtract(balance);
                if (itemsPriceAfterCashback.compareTo(amountOfTransaction) > 0) {
                    throw new InsufficientFundsException("Insufficient funds");
                }
                actualCashback = balance;
            } else {
                actualCashback = cashbackToUse;
            }

            LocalDateTime redeemTimeStamp = LocalDateTime.now();
            wallet.setBalance(wallet.getBalance().subtract(actualCashback));
            wallet.setUpdatedAt(redeemTimeStamp);

            WalletTransactionDomain redeemCashback = WalletTransactionDomain.createWalletTransaction(
                    wallet.getId(),
                    transactionId,
                    balance.subtract(actualCashback),
                    TransactionType.DEBIT,
                    redeemTimeStamp,
                    "Redeem cashback"
            );

            walletTransactionRepository.save(walletTransactionMapper.domainToEntity(redeemCashback));
        } else {
            if (amountOfTransaction.compareTo(totalItemPrice) < 0) {
                throw new InsufficientFundsException("Insufficient funds");
            }
        }

        LocalDateTime timestamp = LocalDateTime.now();
        wallet.setBalance(wallet.getBalance().add(amount));
        wallet.setUpdatedAt(timestamp);

        WalletTransactionDomain walletTransaction = WalletTransactionDomain.createWalletTransaction(
                wallet.getId(),
                transactionId,
                amount,
                TransactionType.CREDIT,
                timestamp,
                "Cashback from transaction " + transactionId
        );

        walletTransactionRepository.save(walletTransactionMapper.domainToEntity(walletTransaction));

        log.info("Points credited: user {} || transaction {} || amount of transaction {}", userId, transactionId, amount);
    }

    @Transactional
    public void blockWallet(Long userId) {
        Wallet wallet = walletRepository
                .findWalletByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(String.format("Wallet by user id %s not found", userId)));

        wallet.setStatus(WalletStatus.BLOCKED);
        wallet.setUpdatedAt(LocalDateTime.now());
        log.info("Wallet {} blocked", wallet.getId());
    }

    @Transactional
    public void unblockWallet(Long userId) {
        Wallet wallet = walletRepository
                .findWalletByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(String.format("Wallet by user id %s not found", userId)));

        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setUpdatedAt(LocalDateTime.now());
        log.info("Wallet {} unblocked", wallet.getId());
    }

    public BigDecimal getBalance(Long userId) {
        return walletRepository
                .findWalletByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(String.format("Wallet by user id %s not found", userId)))
                .getBalance();
    }

    public List<WalletTransactionDomain> getTransactionsHistory(Long userId) {
        UUID walletId = walletRepository
                .findWalletByUserId(userId)
                .orElseThrow(() -> new WalletNotFoundException(String.format("Wallet by user id %s not found", userId)))
                .getId();

        return walletTransactionRepository.getWalletTransactionsByWalletId(walletId)
                .stream()
                .map(walletTransactionMapper::entityToDomain)
                .toList();
    }
}
