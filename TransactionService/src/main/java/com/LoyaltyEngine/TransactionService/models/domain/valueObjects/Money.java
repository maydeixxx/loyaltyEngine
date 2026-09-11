package com.LoyaltyEngine.TransactionService.models.domain.valueObjects;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    public Money {
        Objects.requireNonNull(amount, "Amount cant be null");
        Objects.requireNonNull(currency, "Currency cant be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) throw new IllegalArgumentException("Amount cant be negative");
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money zeroOf(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public void checkCurrency(Money money) {
        if (!money.currency.equals(this.currency)) throw new IllegalArgumentException("Mismatch currency");
    }

    public Money add(Money money) {
        checkCurrency(money);
        return new Money(this.amount.add(money.amount), money.currency);
    }

    public Money subtract(Money money) {
        checkCurrency(money);
        return new Money(this.amount.subtract(money.amount), this.currency);
    }

    public boolean isGreaterThan(Money money) {
        checkCurrency(money);
        return this.amount.compareTo(money.amount) > 0;
    }

    public boolean isLessThan(Money money) {
        checkCurrency(money);
        return this.amount.compareTo(money.amount) < 0;
    }

}
