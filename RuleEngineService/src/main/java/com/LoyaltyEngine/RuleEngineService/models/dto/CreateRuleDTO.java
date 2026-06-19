package com.LoyaltyEngine.RuleEngineService.models.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class CreateRuleDTO {
    @NotNull(message = "Категория обязательна к указанию")
    private final String category;
    @NotNull(message = "Процент обязателен к указанию")
    @DecimalMin(value = "1.0", message = "Процент правила должен быть больше 1")
    private final BigDecimal percentage;
    @NotNull(message = "Дата старта правила обязательна к указанию")
    private final LocalDateTime validFrom;
    @NotNull(message = "Дата окончания правила обязательна к указанию")
    private final LocalDateTime validTo;
}
