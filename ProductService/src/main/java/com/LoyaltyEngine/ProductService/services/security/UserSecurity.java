package com.LoyaltyEngine.ProductService.services.security;

import java.util.Objects;

public record UserSecurity(java.util.UUID userId) {
    public UserSecurity {
        Objects.requireNonNull(userId);
    }
}
