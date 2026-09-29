package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserDTO(
        @Size(min = 2, max = 50, message = "firstName must be between 2 and 50 characters")
        String firstName,
        @Size(min = 2, max = 50, message = "lastName must be between 2 and 50 characters")
        String lastName
) {}
