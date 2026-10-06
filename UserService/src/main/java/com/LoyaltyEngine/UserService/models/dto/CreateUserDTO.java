package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserDTO(
        @NotBlank(message = "Email cant be blank")
        @Email(message = "not valid email")
        String email,

        @NotBlank(message = "First name cant be blank")
        String firstName,

        @NotBlank(message = "Last name cant be blank")
        String lastName,

        @NotBlank(message = "Password cant be blank")
        @Size(min = 8, max = 30, message = "Minimal length of password is 8 symbols")
        String password
) {}
