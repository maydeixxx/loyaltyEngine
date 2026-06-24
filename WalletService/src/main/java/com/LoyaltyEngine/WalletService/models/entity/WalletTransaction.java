package com.LoyaltyEngine.WalletService.models.entity;

import com.LoyaltyEngine.WalletService.models.domain.TransactionType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID walletId;
    private UUID transactionId;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime createdAt;
    private String description;
}
