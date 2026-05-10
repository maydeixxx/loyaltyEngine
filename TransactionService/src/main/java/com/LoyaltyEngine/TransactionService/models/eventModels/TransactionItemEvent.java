package com.LoyaltyEngine.TransactionService.models.eventModels;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TransactionItemEvent {
    private String category;
    private String name;
    private BigDecimal price;
}
