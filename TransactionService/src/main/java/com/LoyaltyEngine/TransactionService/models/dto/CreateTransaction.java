package com.LoyaltyEngine.TransactionService.models.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Getter
public class CreateTransaction {
    @NotNull(message = "Id пользователя обязательно")
    @Min(value = 1, message = "Id пользователя не может быть меньше 1")
    private final Long userId;
    @NotNull(message = "Сумма транзакции обязательна")
    @DecimalMin(value = "0.01", message = "Сумма транзакции должна быть > 0.01")
    private final BigDecimal amount;
    @NotNull(message = "Items обязательны")
    @Size(min = 1, message = "Нужен хотя бы один item")
    @Valid
    private final List<CreateTransactionItem> items;
}
