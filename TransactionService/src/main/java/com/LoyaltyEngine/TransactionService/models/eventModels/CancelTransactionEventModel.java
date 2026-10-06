package com.LoyaltyEngine.TransactionService.models.eventModels;

import java.math.BigDecimal;
import java.util.UUID;

public record CancelTransactionEventModel(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        Boolean useCashback
) {
}
