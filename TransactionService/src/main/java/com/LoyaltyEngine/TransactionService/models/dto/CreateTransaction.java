package com.LoyaltyEngine.TransactionService.models.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateTransaction(
        @NotNull(message = "User id cant be null")
        @Min(value = 1, message = "User id cant be less than 1")
        Long userId,
        @NotNull(message = "Amount of the trans. cant be null")
        @DecimalMin(value = "0.01", message = "Amount of the trans. must be > 0.01")
        BigDecimal amount,
        @NotNull(message = "Items cant be null")
        @Size(min = 1, message = "Items size must be >= 1")
        @Valid
        List<CreateTransactionItem> items,
        @NotNull(message = "Please choose variable of payment")
        Boolean useCashbackBalance
) {}
