package com.LoyaltyEngine.UserService.models.domain.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "User id cant be null");
    }

    public static UserId generateUserId() {
        return new UserId(UuidCreator.getTimeOrderedEpoch());
    }

    public static UserId restoreFromExisting(UUID value) {
        return new UserId(value);
    }

    public static UserId fromString(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
