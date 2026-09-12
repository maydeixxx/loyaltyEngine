package com.LoyaltyEngine.WalletService.models.domain.valueObjects;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId cant be null");
    }

}
