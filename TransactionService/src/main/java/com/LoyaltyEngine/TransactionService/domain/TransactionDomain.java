package com.LoyaltyEngine.TransactionService.domain;

import com.LoyaltyEngine.TransactionService.entity.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDomain {
    private UUID id;
    private Long userId;
    private UUID idempotencyKey;
    private BigDecimal amount;
    private LocalDateTime createdAt;
    private Status status;
}
