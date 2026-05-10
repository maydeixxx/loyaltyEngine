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
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private Long userId;
    private UUID idempotencyKey;
    private BigDecimal amount;
    @OneToMany(
            fetch = FetchType.LAZY,
            mappedBy = "transaction",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<TransactionItem> transactionItems = new ArrayList<>();
    private LocalDateTime createdAt;
    private Status status;

    public void setTransactionItems(List<TransactionItem> items) {
        this.transactionItems.clear();
        if (items != null) {
            items.forEach(this::addTransactionItem);
        }
    }

    public void addTransactionItem(TransactionItem item) {
        if (item != null) {
            transactionItems.add(item);
            item.setTransaction(this);
        }
    }
}
