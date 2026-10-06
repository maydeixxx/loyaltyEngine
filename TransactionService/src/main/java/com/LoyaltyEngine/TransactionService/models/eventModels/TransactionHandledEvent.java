package com.LoyaltyEngine.TransactionService.models.eventModels;

import java.util.UUID;

public record TransactionHandledEvent(
        UUID transactionId,
        UUID userId
) {
}
