package com.LoyaltyEngine.UserService.models.domain;

import com.LoyaltyEngine.UserService.exceptions.UserValidationException;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class UserDomain {
    @Setter
    private Long id;
    private final String email;
    private final String firstName;
    private final String lastName;
    private final String password;
    private final Role role;
    private final LocalDateTime createdAt;
    @Setter
    private LocalDateTime updatedAt;

    public static UserDomain createUser(String email, String firstName, String lastName, String password) {
        if (email == null || email.isEmpty()) {
            throw new UserValidationException("Email cant be null or empty");
        }

        if (firstName == null || firstName.isEmpty()) {
            throw new UserValidationException("firstName cant be null or empty");
        }

        if (lastName == null || lastName.isEmpty()) {
            throw new UserValidationException("lastName cant be null or empty");
        }

        if (password == null || password.isEmpty()) {
            throw new UserValidationException("password cant be null or empty");
        }

        return new UserDomain(email, firstName, lastName, password, Role.USER, LocalDateTime.now());
    }
}
