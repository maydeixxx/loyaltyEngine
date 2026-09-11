package com.LoyaltyEngine.TransactionService.models.domain.valueObjects;

import java.util.Objects;
import java.util.UUID;

public record IdempotencyKey(UUID value) {

    public IdempotencyKey {
        Objects.requireNonNull(value, "Idempotency key cant be null");
    }

    public static IdempotencyKey restoreFromExisting(UUID value) {
        return new IdempotencyKey(value);
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(UUID.fromString(value));
    }

}
