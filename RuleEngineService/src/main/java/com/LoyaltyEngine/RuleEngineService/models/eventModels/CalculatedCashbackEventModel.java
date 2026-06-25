package com.LoyaltyEngine.RuleEngineService.models.eventModels;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
public class CalculatedCashbackEventModel {
    private UUID transactionId;
    private Long userId;
    private BigDecimal amount;
}
