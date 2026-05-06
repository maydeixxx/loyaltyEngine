package com.LoyaltyEngine.TransactionService.models.domain;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private final LocalDateTime createdAt;
    private final Status status;

    public static TransactionDomain create(Long userId, BigDecimal amount, UUID idempotencyKey) {
        if (userId == null) {
            throw new IllegalArgumentException("UserId не может быть null");
        }
        if (userId < 1) {
            throw new IllegalArgumentException("UserId не может быть меньше 1");
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount не может быть меньше 0 или равной 0");
        }

        if (idempotencyKey == null) {
            throw new IllegalArgumentException("Idempotency key не может быть null");
        }

        return new TransactionDomain(userId, idempotencyKey, amount, LocalDateTime.now(), Status.NEW);
    }
}
