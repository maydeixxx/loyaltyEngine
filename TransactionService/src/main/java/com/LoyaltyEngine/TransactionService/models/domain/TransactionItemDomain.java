package com.LoyaltyEngine.TransactionService.models.domain;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class TransactionItemDomain {
    @Setter
    private UUID id;
    private final String category;
    private final String name;
    private final BigDecimal price;

    public static TransactionItemDomain createTransactionItem(String category, String name, BigDecimal price) {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Category не может быть пустым");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name не может быть пустым");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price не может быть <= 0 или null");
        }

        return new TransactionItemDomain(category, name, price);
    }
}
