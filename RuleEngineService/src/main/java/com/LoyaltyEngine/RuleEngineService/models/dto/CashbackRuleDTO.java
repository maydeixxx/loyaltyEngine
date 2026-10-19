package com.LoyaltyEngine.RuleEngineService.models.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CashbackRuleDTO(
        UUID id,
        String category,
        BigDecimal percentage,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {
}
