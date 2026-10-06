package com.LoyaltyEngine.WalletService.models.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record CancellationFailedEvent(
        UUID transactionId,
        UUID userId,
        String cause,
        LocalDateTime failedAt
) {
}
