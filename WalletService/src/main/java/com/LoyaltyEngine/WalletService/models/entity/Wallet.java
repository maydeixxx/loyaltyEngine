package com.LoyaltyEngine.WalletService.models.entity;

import com.LoyaltyEngine.WalletService.models.domain.WalletStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Entity
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private Long userId;
    private BigDecimal balance;
    private WalletStatus status;
    @Version
    private Long version;
    private LocalDateTime updatedAt;
}
