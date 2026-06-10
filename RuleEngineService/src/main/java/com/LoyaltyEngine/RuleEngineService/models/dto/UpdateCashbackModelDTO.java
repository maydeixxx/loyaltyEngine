package com.LoyaltyEngine.RuleEngineService.models.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateCashbackModelDTO(
        String category,
        BigDecimal percentage,
        LocalDateTime validTo,
        LocalDateTime validFrom
) {}
