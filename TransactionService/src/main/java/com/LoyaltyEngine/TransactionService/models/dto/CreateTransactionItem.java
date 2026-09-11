package com.LoyaltyEngine.TransactionService.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Currency;

public record CreateTransactionItem (
    @NotBlank(message = "Категория не может быть пустой")
    String category,

    @NotBlank(message = "Имя не может быть пустым")
    String name,

    @NotNull(message = "Price обязательна")
    @DecimalMin(message = "Price не может быть < 0.01", value = "0.01")
    BigDecimal price,

    @NotNull(message = "Currency is required")
    Currency currency
) {}
