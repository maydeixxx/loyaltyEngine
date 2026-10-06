package com.LoyaltyEngine.TransactionService.models.eventModels;

import java.math.BigDecimal;

public record TransactionItemEvent(
        String category,
        String name,
        BigDecimal price
) {
}
