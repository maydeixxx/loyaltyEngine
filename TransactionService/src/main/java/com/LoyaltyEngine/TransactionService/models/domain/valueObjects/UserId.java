package com.LoyaltyEngine.TransactionService.models.domain.valueObjects;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "User id cant be null");
    }

    public static UserId restoreFromExisting(UUID value) {
        return new UserId(value);
    }

}
