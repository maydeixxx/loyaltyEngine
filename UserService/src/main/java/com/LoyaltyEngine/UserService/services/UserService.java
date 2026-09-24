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
import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import com.LoyaltyEngine.UserService.services.interfaces.OutboxEventRepository;
import com.LoyaltyEngine.UserService.services.interfaces.UserMapper;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.LoyaltyEngine.UserService.services.security.JwtService;
import com.github.f4b6a3.uuid.UuidCreator;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.ScopedSpan;
import io.micrometer.tracing.Tracer;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    @Value("${kafka.topics.user-created}")
    private String userCreatedTopic;

    private final OutboxEventRepository outboxEventRepository;

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;

    private final MeterRegistry registry;
    private final Tracer tracer;

    @Transactional
    public UserDomain createUser(CreateUserDTO userDTO) {
        ScopedSpan span = tracer.startScopedSpan("create-user-span");
        span.tag("user.email", userDTO.email());
        Timer.Sample sample = Timer.start(registry);

        try {
            if (userRepository.findUserByEmail(userDTO.email()).isPresent()) throw new CreateUserException("User with email [%s] exists".formatted(userDTO.email()));

            UserDomain newUser = UserDomain.createUser(userDTO.email(), userDTO.firstName(), userDTO.lastName(), passwordEncoder.encode(userDTO.password()));
            userRepository.saveAndFlush(userMapper.domainToEntity(newUser));

            OutboxEvent event = OutboxEvent.builder()
                    .id(UuidCreator.getTimeOrderedEpoch())
                    .eventType(userCreatedTopic)
                    .retryCount(0)
                    .status(OutboxStatus.NEW)
                    .aggregateId(newUser.getId().value())
                    .createdAt(LocalDateTime.now())
                    .payload(newUser.getId().value().toString())
                    .build();

            outboxEventRepository.save(event);

            registry.counter("loyalty.users.registered", "status", "successful").increment();
            span.tag("status", "SUCCESSFUL");
            return newUser;
        } catch (CreateUserException e) {
            registry.counter("loyalty.users.registered", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            registry.counter("loyalty.users.registered", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error("Unexpected error creating user: {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            span.end();
            sample.stop(registry.timer("loyalty.users.registration.timer"));
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

    @Transactional
    public void updateUser(String email, UpdateUserDTO updateUserDTO) {
        try {
            if (updateUserDTO.fieldToUpdate() == null) {
                throw new NullPointerException("Field to update is required");
            }

            UserDomain user = userMapper.entityToDomain(userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));

            switch (updateUserDTO.fieldToUpdate().toLowerCase().trim()) {
                case "email" -> user.updateEmail(updateUserDTO.email());
                case "lastname" -> user.updateLastName(updateUserDTO.lastName());
                case "firstname" -> user.updateFirstName(updateUserDTO.firstName());
                case "password" -> {
                    if (updateUserDTO.oldPassword() == null || updateUserDTO.oldPassword().isBlank() || !passwordEncoder.matches(updateUserDTO.oldPassword(), user.getPasswordHash().value())) throw new UserUpdateException("Password null or incorrect");
                    if (passwordEncoder.matches(updateUserDTO.newPassword(), user.getPasswordHash().value())) throw new IllegalArgumentException("New password cant be the same as old");

                    user.updatePassword(passwordEncoder.encode(updateUserDTO.newPassword()));
                }
                default -> throw new UserUpdateException("Unknown field to update");
            }

            user.updateUpdatedAt(LocalDateTime.now());
            userRepository.save(userMapper.domainToEntity(user));
        } catch (UserNotFoundException | UserUpdateException | NullPointerException e) {
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
