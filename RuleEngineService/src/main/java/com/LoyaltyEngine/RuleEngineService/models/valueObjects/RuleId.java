package com.LoyaltyEngine.RuleEngineService.models.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record RuleId(UUID value) {
    public RuleId {
        Objects.requireNonNull(value, "Id cant be null");
    }

    public static RuleId generateId() {
        return new RuleId(UuidCreator.getTimeOrderedEpoch());
    }
}
