package com.LoyaltyEngine.WalletService.models.domain.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record WalletId(UUID value) {

    public WalletId {
        Objects.requireNonNull(value, "Wallet id cant be null");
    }

    public static WalletId generateId() {
        return new WalletId(UuidCreator.getTimeOrderedEpoch());
    }

    public static WalletId restoreFromExisting(UUID walletId) {
        return new WalletId(walletId);
    }

}
