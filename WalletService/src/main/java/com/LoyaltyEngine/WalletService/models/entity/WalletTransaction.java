package com.LoyaltyEngine.WalletService.models.entity;

import com.LoyaltyEngine.WalletService.models.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "wallet_transactions")
@NoArgsConstructor
@Setter
@Getter
public class WalletTransaction {
    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;
    @Column(nullable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private String description;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WalletTransaction that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
