package com.LoyaltyEngine.WalletService.services;

import com.LoyaltyEngine.WalletService.exceptions.WalletBlockedException;
import com.LoyaltyEngine.WalletService.exceptions.WalletNotFoundException;
import com.LoyaltyEngine.WalletService.models.domain.TransactionType;
import com.LoyaltyEngine.WalletService.models.domain.WalletDomain;
import com.LoyaltyEngine.WalletService.models.domain.WalletStatus;
import com.LoyaltyEngine.WalletService.models.domain.WalletTransactionDomain;
import com.LoyaltyEngine.WalletService.models.entity.Wallet;
import com.LoyaltyEngine.WalletService.models.events.PointsFailedEvent;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletRepository;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionMapper;
import com.LoyaltyEngine.WalletService.services.interfaces.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletService {
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;
    @Qualifier("pointsFailedEventKafkaTemplate")
    private final KafkaTemplate<UUID, PointsFailedEvent> kafkaTemplate;

    private Wallet createWallet(Long userId) {
        return walletRepository.save(walletMapper.domainToEntity(WalletDomain.createWallet(userId)));
    }

    @Transactional
    public void creditPoints(Long userId, UUID transactionId, BigDecimal amount) {
        Wallet wallet = walletRepository
                .findWalletByUserId(userId)
                .orElseGet(() -> createWallet(userId));

        if (wallet.getStatus() == WalletStatus.BLOCKED) {
            throw new WalletBlockedException(String.format("Wallet %s blocked.", wallet.getId()));
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
}
