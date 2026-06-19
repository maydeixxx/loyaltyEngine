package com.LoyaltyEngine.RuleEngineService.models.eventModels;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class CalculatedCashbackEventModel {
    private Long userId;
    private BigDecimal amount;
}
