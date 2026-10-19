package com.LoyaltyEngine.ProductService.services.security;

import java.util.Objects;
import java.util.UUID;

public record UserSecurity(UUID userId, String role) {
    public UserSecurity {
        Objects.requireNonNull(userId, "User id cant be null");
        Objects.requireNonNull(role, "User role cant be null");
    }

    public boolean isAdmin() {
        return "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);
    }
}
