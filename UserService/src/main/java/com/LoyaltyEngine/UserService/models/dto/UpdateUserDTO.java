package com.LoyaltyEngine.UserService.models.dto;

import com.LoyaltyEngine.UserService.models.domain.enums.FieldToUpdate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserDTO(
        FieldToUpdate fieldToUpdate,

        @Email(message = "email is not valid")
        String email,

        @Size(min = 2, max = 50, message = "firstName must be between 2 and 50 characters")
        String firstName,
        @Size(min = 2, max = 50, message = "lastName must be between 2 and 50 characters")
        String lastName,

        @Size(min = 5, max = 10, message = "password must be between 5 and 10 characters")
        String password
) {}
