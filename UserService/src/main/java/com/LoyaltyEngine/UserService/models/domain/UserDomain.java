package com.LoyaltyEngine.UserService.models.domain;

import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.exceptions.UserValidationException;
import com.LoyaltyEngine.UserService.models.domain.enums.Role;
import com.LoyaltyEngine.UserService.models.domain.valueObjects.HashedPassword;
import com.LoyaltyEngine.UserService.models.domain.valueObjects.UserId;
import lombok.*;

import java.time.LocalDateTime;

@Getter
public class UserDomain {
    private final UserId id;
    private String email;
    private String firstName;
    private String lastName;
    private HashedPassword passwordHash;
    private Role role;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UserDomain(UserId id, String email, String firstName, String lastName, HashedPassword passwordHash, Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

        UserId userId = UserId.generateUserId();
        LocalDateTime now = LocalDateTime.now();
        HashedPassword hashedPassword = new HashedPassword(password);

        return new UserDomain(userId, email, firstName, lastName, hashedPassword, Role.USER, now, now);
    }

    public void updateEmail(String newEmail) {
        if (newEmail == null) throw new UserUpdateException("New email cant be null");
        if (this.email.equals(newEmail)) throw new UserUpdateException("You cant enter the same email");

        this.email = newEmail;
    }

    public void updateFirstName(String newFirstName) {
        if (newFirstName == null) throw new UserUpdateException("New first name cant be null");
        if (this.firstName.equals(newFirstName)) throw new UserUpdateException("You already have this first name");

        this.firstName = newFirstName;
    }

    public void updateLastName(String newLastName) {
        if (newLastName == null) throw new UserUpdateException("New last name cant be null");
        if (this.lastName.equals(newLastName)) throw new UserUpdateException("You already have this last name");

        this.lastName = newLastName;
    }

    public void updatePassword(String newPasswordHash) {
        HashedPassword newHashedPassword = new HashedPassword(newPasswordHash);
        if (this.passwordHash.equals(newHashedPassword)) throw new UserUpdateException("You already have this password");

        this.passwordHash = newHashedPassword;
    }

    public void updateRole(Role newRole) {
        if (newRole == null) throw new UserUpdateException("New role cant be null");
        if (this.role.equals(newRole)) throw new UserUpdateException("User is already [%s]".formatted(newRole));

        this.role = newRole;
    }

    public void updateUpdatedAt(LocalDateTime newUpdatedAt) {
        this.updatedAt = newUpdatedAt;
    }
}
