package com.LoyaltyEngine.ProductService.models.domain.valueObjects;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "User id cant be null");
    }

}
