package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordDTO(
        @NotBlank(message = "Old password cant be null or blank")
        @Size(min = 8, max = 30, message = "Minimal length of password is 8 symbols")
        String oldPassword,

        @NotBlank(message = "New password cant be null or blank")
        @Size(min = 8, max = 30, message = "Minimal length of new password is 8 symbols")
        String newPassword
) {}
