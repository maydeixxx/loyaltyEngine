package com.LoyaltyEngine.TransactionService.models.domain;

import com.LoyaltyEngine.TransactionService.models.domain.valueObjects.ItemId;
import com.LoyaltyEngine.TransactionService.models.domain.valueObjects.Money;
import lombok.*;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

@Getter
public class TransactionItemDomain {
    private final ItemId id;
    private final String category;
    private final String name;
    private final Money price;

    private TransactionItemDomain(ItemId id, String category, String name, Money price) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.price = price;
    }

    public static TransactionItemDomain createTransactionItem(String category, String name, BigDecimal amount, Currency currency) {
        Money price = new Money(amount, currency);

        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category не может быть пустым");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name не может быть пустым");
        }

        ItemId itemId = ItemId.generateItemId();
        return new TransactionItemDomain(itemId, category, name, price);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TransactionItemDomain that)) return false;
        return Objects.equals(id.value(), that.id.value());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id.value());
    }
}
