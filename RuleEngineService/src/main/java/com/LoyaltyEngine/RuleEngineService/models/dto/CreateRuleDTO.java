package com.LoyaltyEngine.RuleEngineService.models.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateRuleDTO(
        @NotNull(message = "Category cant be null")
        @Size(min = 1, message = "Category cant be empty")
        String category,
        @NotNull(message = "Rule percentage cant be null")
        @Positive(message = "Rule percentage cant be negative")
        BigDecimal percentage,
        @NotNull(message = "Valid from cant be null")
        LocalDateTime validFrom,
        @NotNull(message = "Valid to cant be null")
        LocalDateTime validTo)
{}
