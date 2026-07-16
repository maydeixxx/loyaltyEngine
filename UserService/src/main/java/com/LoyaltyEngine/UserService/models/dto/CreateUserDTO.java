package com.LoyaltyEngine.UserService.models.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class CreateUserDTO {
    @NotNull(message = "email cant be null")
    @NotEmpty(message = "firstName cant be empty")
    @Email(message = "not valid email")
    private String email;

    @NotNull(message = "firstName cant be null")
    @NotEmpty(message = "firstName cant be empty")
    private String firstName;

    @NotNull(message = "lastName cant be null")
    @NotEmpty(message = "lastName cant be empty")
    private String lastName;

    @NotNull(message = "password cant be null")
    @NotEmpty(message = "password cant be empty")
    @Length(min = 8, message = "Minimal length of password is 8 symbols")
    private String password;
}
