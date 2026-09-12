package com.LoyaltyEngine.RuleEngineService.models.eventModels;


import java.math.BigDecimal;


public record TransactionItemEvent(
        String category,
        String name,
        BigDecimal price
) {
}
