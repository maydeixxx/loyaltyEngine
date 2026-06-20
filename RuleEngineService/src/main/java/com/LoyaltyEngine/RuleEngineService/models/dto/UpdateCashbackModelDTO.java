package com.LoyaltyEngine.RuleEngineService.models.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateCashbackModelDTO(
        @Size(min = 1, message = "Category cant be empty")
        String category,
        @Positive(message = "percentage cant be negative")
        BigDecimal percentage,
        LocalDateTime validTo,
        LocalDateTime validFrom
)
{}
