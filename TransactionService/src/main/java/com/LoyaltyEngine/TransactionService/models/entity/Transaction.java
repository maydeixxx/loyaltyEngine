package com.LoyaltyEngine.TransactionService.models.entity;

import com.LoyaltyEngine.TransactionService.models.domain.Status;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transaction", indexes = {
        @Index(name = "idx_idempotency_key", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_user_id", columnList = "userId")
})
@Data
public class Transaction {
    @Id
    private UUID id;

    private UUID userId;
    private UUID idempotencyKey;

    @Column(nullable = false)
    private BigDecimal amount;
    @Column(nullable = false)
    private String currency;

    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "transaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<TransactionItem> transactionItems = new ArrayList<>();

    private LocalDateTime createdAt;
    private Status status;

    private Boolean useCashbackBalance;

    public void addItem(TransactionItem item) {
        this.transactionItems.add(item);
        item.setTransaction(this);
    }
}
