package com.LoyaltyEngine.TransactionService.models.eventModels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointsFailedEvent {
    private Long userId;
    private BigDecimal amount;
    private String cause;
    private LocalDateTime failedAt;
}
