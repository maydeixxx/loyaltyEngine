package com.LoyaltyEngine.TransactionService.models.dto;

import java.math.BigDecimal;


public record TransactionItemDTO (
    String category,
    String name,
    BigDecimal price
) {}
