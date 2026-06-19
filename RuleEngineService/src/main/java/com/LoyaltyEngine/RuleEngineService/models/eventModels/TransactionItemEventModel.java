package com.LoyaltyEngine.RuleEngineService.models.eventModels;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionItemEventModel {
    private String category;
    private String name;
    private BigDecimal price;
}
