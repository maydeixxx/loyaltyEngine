package com.LoyaltyEngine.TransactionService.models.dto;

import com.LoyaltyEngine.TransactionService.models.domain.Status;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class TransactionDTO {
    UUID id;
    UUID userId;
    UUID idempotencyKey;
    BigDecimal amount;
    List<TransactionItemDTO> items;
    LocalDateTime createdAt;
    Status status;
}
