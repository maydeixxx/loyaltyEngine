package com.LoyaltyEngine.WalletService.models.events;

import java.math.BigDecimal;
import java.util.UUID;

public record CancelTransactionEventModel(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        Boolean useCashback
) {
}
