package com.LoyaltyEngine.TransactionService.models.dto;

import lombok.Builder;
import lombok.Data;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class TransactionItemDTO {
    String category;
    String name;
    BigDecimal price;
}
