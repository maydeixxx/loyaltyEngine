package com.LoyaltyEngine.WalletService.models.domain.valueObjects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal amount) {

    public Money {
        Objects.requireNonNull(amount, "Amount cant be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Amount cant be less than zero");
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static Money zeroOf() {
        return new Money(BigDecimal.ZERO);
    }

    public Money add(Money money) {
        return new Money(this.amount.add(money.amount));
    }

    public Money subtract(Money money) {
        return new Money(this.amount.subtract(money.amount));
    }

    public boolean isGreaterThan(Money money) {
        return this.amount.compareTo(money.amount) > 0;
    }

    public boolean isLessThan(Money money) {
        return this.amount.compareTo(money.amount) < 0;
    }

}
