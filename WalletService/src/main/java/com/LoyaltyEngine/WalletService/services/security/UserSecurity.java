package com.LoyaltyEngine.WalletService.services.security;

import java.util.Objects;
import java.util.UUID;

public record UserSecurity(String email, UUID userId) {
    public UserSecurity {
        Objects.requireNonNull(email);
        Objects.requireNonNull(userId);
    }
}
