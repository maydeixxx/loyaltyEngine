package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.AuthenticationException;
import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.models.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.services.interfaces.UserMapper;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.LoyaltyEngine.UserService.services.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserDomain createUser(CreateUserDTO userDTO) {
        try {
            UserDomain newUser = UserDomain.createUser(userDTO.email(), userDTO.firstName(), userDTO.lastName(), passwordEncoder.encode(userDTO.password()));
            return userMapper.entityToDomain(userRepository.save(userMapper.domainToEntity(newUser)));
        } catch (DataIntegrityViolationException e) {
            log.error("Email {} already registered", userDTO.email());
            throw new CreateUserException("Email [%s] already registered".formatted(userDTO.email()));
        } catch (Exception e) {
            log.error("Unexpected error creating user: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void deleteUser(String email) {
        try {
            User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email))));
            userRepository.delete(user);
        } catch (UserNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public UserDomain findUserById(UUID id) {
        try {
            return userMapper.entityToDomain(
                    userRepository
                            .findUserById(id)
                            .orElseThrow(() -> new UserNotFoundException((String.format("User by id [%s] not found", id))))
            );
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error getting user by id: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public List<UserDomain> getAllUsers() {
        try {
            return userRepository.findAll()
                    .stream()
                    .map(userMapper::entityToDomain)
                    .toList();
        } catch (Exception e) {
            log.error("Error getting all users: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void updateUser(String email, UpdateUserDTO updateUserDTO) {
        try {
            UserDomain user = userMapper.entityToDomain(userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));

            switch (updateUserDTO.fieldToUpdate()) {
                case EMAIL -> user.updateEmail(updateUserDTO.email());
                case LAST_NAME -> user.updateLastName(updateUserDTO.lastName());
                case FIRST_NAME -> user.updateFirstName(updateUserDTO.firstName());
                case PASSWORD -> user.updatePassword(passwordEncoder.encode(updateUserDTO.password()));
                default -> throw new UserUpdateException("Unknown field to update");
            }

            user.updateUpdatedAt(LocalDateTime.now());
            userRepository.save(userMapper.domainToEntity(user));
        } catch (UserNotFoundException | UserUpdateException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public UserDomain findUserByEmail(String email) {
        try {
            return userMapper.entityToDomain(userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));
        } catch (UserNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error getting user by email: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public String login(AuthUserDto userDto) {
        try {
            String email = userDto.email();
            String password = userDto.password();

            User user = userRepository.findUserByEmail(email)
                    .orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email))));

            if (!passwordEncoder.matches(password, user.getHashedPassword())) {
                throw new AuthenticationException("Password is incorrect");
            }
            return jwtService.generateJwtToken(user);
        } catch (UserNotFoundException | AuthenticationException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error logging in: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
