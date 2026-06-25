package com.LoyaltyEngine.WalletService.services.interfaces;

import com.LoyaltyEngine.WalletService.models.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    List<WalletTransaction> getWalletTransactionsByWalletId(UUID walletId);
}
