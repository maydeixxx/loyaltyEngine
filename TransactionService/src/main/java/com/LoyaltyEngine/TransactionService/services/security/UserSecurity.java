package com.LoyaltyEngine.TransactionService.services.security;

import java.util.Objects;

public record UserSecurity(String email, java.util.UUID userId) {
    public UserSecurity {
        Objects.requireNonNull(email);
        Objects.requireNonNull(userId);
    }
}
