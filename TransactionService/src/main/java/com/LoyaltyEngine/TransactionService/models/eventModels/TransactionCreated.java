package com.LoyaltyEngine.TransactionService.models.eventModels;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TransactionCreated {
    private Long userId;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
