package com.LoyaltyEngine.UserService.services.interfaces;

import com.LoyaltyEngine.UserService.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserById(UUID id);

    Optional<User> findUserByEmail(String email);
}
