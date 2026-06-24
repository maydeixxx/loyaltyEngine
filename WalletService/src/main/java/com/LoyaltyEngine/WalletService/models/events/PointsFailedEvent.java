package com.LoyaltyEngine.WalletService.models.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PointsFailedEvent {
    private Long userId;
    private BigDecimal amount;
    private String cause;
    private LocalDateTime failedAt;
}
