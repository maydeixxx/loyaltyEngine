package com.LoyaltyEngine.TransactionService.models.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private Long userId;
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID idempotencyKey;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private Status status;
}
