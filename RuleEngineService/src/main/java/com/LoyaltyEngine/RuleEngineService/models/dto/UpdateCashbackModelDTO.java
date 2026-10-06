package com.LoyaltyEngine.RuleEngineService.models.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateCashbackModelDTO(
        @Positive(message = "percentage cant be negative")
        @Min(value = 1, message = "New percentage cant be less than 1")
        BigDecimal percentage,
        LocalDateTime validTo
)
{}
