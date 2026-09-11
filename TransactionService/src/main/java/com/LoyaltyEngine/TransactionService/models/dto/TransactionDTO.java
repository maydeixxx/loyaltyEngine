package com.LoyaltyEngine.TransactionService.models.dto;

import com.LoyaltyEngine.TransactionService.models.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


public record TransactionDTO (
    UUID id,
    UUID userId,
    UUID idempotencyKey,
    BigDecimal amount,
    List<TransactionItemDTO> items,
    LocalDateTime createdAt,
    Status status
) {}
