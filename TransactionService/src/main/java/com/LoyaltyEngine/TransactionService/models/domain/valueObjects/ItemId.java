package com.LoyaltyEngine.TransactionService.models.domain.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record ItemId(UUID value) {

    public ItemId {
        Objects.requireNonNull(value, "Item id cant be null");
    }

    public static ItemId generateItemId() {
        return new ItemId(UuidCreator.getTimeOrderedEpoch());
    }

    public ItemId fromString(String value) {
        return new ItemId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
