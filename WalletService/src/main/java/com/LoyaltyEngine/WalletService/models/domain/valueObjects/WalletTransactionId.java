package com.LoyaltyEngine.WalletService.models.domain.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record WalletTransactionId (UUID value) {

    public WalletTransactionId {
        Objects.requireNonNull(value, "Wallet transaction id cant be null");
    }

    public static WalletTransactionId generateId() {
        return new WalletTransactionId(UuidCreator.getTimeOrderedEpoch());
    }

}
