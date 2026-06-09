package com.LoyaltyEngine.RuleEngineService.models.eventModels;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class CalculatedCashbackEventModel {
    private Long userId;
    private BigDecimal amount;
}
