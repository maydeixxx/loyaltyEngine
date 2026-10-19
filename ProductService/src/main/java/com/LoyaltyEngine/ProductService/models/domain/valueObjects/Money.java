package com.LoyaltyEngine.ProductService.models.domain.valueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal value) {
    public Money {
        Objects.requireNonNull(value, "Price cant be null");
        if (value.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Item price cant be negative");

        value = value.setScale(2, RoundingMode.HALF_UP);
    }
}
