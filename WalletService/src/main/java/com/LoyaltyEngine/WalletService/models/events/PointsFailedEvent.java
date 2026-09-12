package com.LoyaltyEngine.WalletService.models.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PointsFailedEvent (
     UUID transactionId,
     UUID userId,
     BigDecimal amount,
     String cause,
     LocalDateTime failedAt
) {}
