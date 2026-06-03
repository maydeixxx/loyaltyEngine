package com.LoyaltyEngine.TransactionService.models.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionItemDTO {
    private String category;
    private String name;
    private BigDecimal price;
}
