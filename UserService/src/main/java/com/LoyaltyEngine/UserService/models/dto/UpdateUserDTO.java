package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUserDTO(
        @NotNull(message = "Field to update is required")
        String fieldToUpdate,

        @Email(message = "email is not valid")
        String email,

        @Size(min = 2, max = 50, message = "firstName must be between 2 and 50 characters")
        String firstName,
        @Size(min = 2, max = 50, message = "lastName must be between 2 and 50 characters")
        String lastName,

        @Size(min = 8, max = 25, message = "password must be between 8 and 25 characters")
        String newPassword,
        String oldPassword
) {}
