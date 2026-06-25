package com.LoyaltyEngine.WalletService.models.events;

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
public class PointsFailedEvent {
    private UUID transactionId;
    private Long userId;
    private BigDecimal amount;
    private String cause;
    private LocalDateTime failedAt;
}
