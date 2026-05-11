package com.LoyaltyEngine.TransactionService.models.eventModels;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionItemEvent {
    private String category;
    private String name;
    private BigDecimal price;
}
