package com.LoyaltyEngine.TransactionService.models.domain.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record TransactionId(UUID value) {

    public TransactionId {
        Objects.requireNonNull(value, "Transaction id cant be null");
    }

    public static TransactionId generateTransactionId() {
        return new TransactionId(UuidCreator.getTimeOrderedEpoch());
    }

    public static TransactionId restoreFromExisting(UUID id) {
        return new TransactionId(id);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
