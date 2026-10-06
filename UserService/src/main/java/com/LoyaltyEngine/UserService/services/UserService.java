package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.models.entity.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.models.entity.OutboxEvent;
import com.LoyaltyEngine.UserService.models.enums.OutboxStatus;
import com.LoyaltyEngine.UserService.services.interfaces.OutboxEventRepository;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
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

            outboxEventRepository.saveAndFlush(event);

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
            UserDomain user = userMapper.entityToDomain(userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));

            if (updateUserDTO.firstName() != null && !updateUserDTO.firstName().isBlank()) user.updateFirstName(updateUserDTO.firstName());
            if (updateUserDTO.lastName() != null && !updateUserDTO.lastName().isBlank()) user.updateLastName(updateUserDTO.lastName());

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
}
