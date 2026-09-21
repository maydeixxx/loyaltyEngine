package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record CreateUserDTO(
        @NotBlank(message = "Email cant be blank")
        @Email(message = "not valid email")
        String email,

        @NotBlank(message = "First name cant be blank")
        String firstName,

        @NotBlank(message = "Last name cant be blank")
        String lastName,

        @NotBlank(message = "Password cant be blank")
        @Length(min = 8, message = "Minimal length of password is 8 symbols")
        String password
) {}
