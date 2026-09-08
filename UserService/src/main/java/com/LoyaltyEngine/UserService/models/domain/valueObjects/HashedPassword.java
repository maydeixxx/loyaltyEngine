package com.LoyaltyEngine.UserService.models.domain.valueObjects;

import com.LoyaltyEngine.UserService.exceptions.UserValidationException;

public record HashedPassword(String value) {

    public HashedPassword {
        if (value == null || value.isBlank()) throw new UserValidationException("Password cant be null or blank");
    }

    @Override
    public String toString() {
        return "PROTECTED";
    }
}
