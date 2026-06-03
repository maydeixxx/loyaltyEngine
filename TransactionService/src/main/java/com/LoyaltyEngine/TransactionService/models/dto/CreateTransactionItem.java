package com.LoyaltyEngine.TransactionService.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class CreateTransactionItem {
    @NotBlank(message = "Категория не может быть пустой")
    private final String category;
    @NotBlank(message = "Имя не может быть пустым")
    private final String name;
    @NotNull(message = "Price обязательна")
    @DecimalMin(message = "Price не может быть < 0.01", value = "0.01")
    private final BigDecimal price;
}
