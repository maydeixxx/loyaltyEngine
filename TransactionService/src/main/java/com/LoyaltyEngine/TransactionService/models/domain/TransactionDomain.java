package com.LoyaltyEngine.TransactionService.models.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@ToString
@EqualsAndHashCode
@RequiredArgsConstructor
public class TransactionDomain {
    @Setter
    private UUID id;
    private final Long userId;
    private final UUID idempotencyKey;
    private final BigDecimal amount;
    private final List<TransactionItemDomain> items;
    private final LocalDateTime createdAt;
    private final Status status;
    private final Boolean useCashbackBalance;

    public static TransactionDomain create(Long userId, UUID idempotencyKey, BigDecimal amount, List<TransactionItemDomain> items, Boolean useCashbackBalance) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cant be null");
        }
        if (userId < 1) {
            throw new IllegalArgumentException("User id cant be less than 1");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount of the trans. cant be null");
        }

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key cant be null");
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Items size must be >= 1");
        }

        BigDecimal totalSum = items.stream()
                .map(TransactionItemDomain::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (amount.compareTo(totalSum) > 0) {
            throw new IllegalArgumentException("Sum of the trans. cant be greater than amount of items");
        }

        if (!useCashbackBalance && (amount.compareTo(totalSum) < 0)) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        return new TransactionDomain(userId, idempotencyKey, amount, items, LocalDateTime.now(), Status.NEW, useCashbackBalance);
    }
}
