package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateEmailDTO(
        @Email(message = "Email is not valid")
        @NotBlank(message = "New email cant be null or blank")
        String newEmail
) {}
