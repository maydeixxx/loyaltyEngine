package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
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
            if (userRepository.findUserByEmail(userDTO.email()).isPresent())
                throw new CreateUserException("User with email [%s] exists".formatted(userDTO.email()));

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
        } catch (Exception e) {
            registry.counter("loyalty.users.registered", "status", "failed").increment();
            span.error(e);
            span.tag("error.message", e.getMessage());
            span.tag("status", "FAILED");

            log.error("Unexpected error creating user: ", e);
            throw e;
        } finally {
            span.end();
            sample.stop(registry.timer("loyalty.users.registration.timer"));
        }
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email))));
        userRepository.delete(user);
    }

    public UserDomain findUserById(UUID id) {
        return userMapper.entityToDomain(
                userRepository
                        .findUserById(id)
                        .orElseThrow(() -> new UserNotFoundException((String.format("User by id [%s] not found", id))))
        );
    }

    public List<UserDomain> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::entityToDomain)
                .toList();
    }

    @Transactional
    public void updateUser(String email, UpdateUserDTO updateUserDTO) {
        UserDomain user = userMapper.entityToDomain(userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));

        if (updateUserDTO.firstName() != null && !updateUserDTO.firstName().isBlank())
            user.updateFirstName(updateUserDTO.firstName());
        if (updateUserDTO.lastName() != null && !updateUserDTO.lastName().isBlank())
            user.updateLastName(updateUserDTO.lastName());

        userRepository.save(userMapper.domainToEntity(user));
    }

    public UserDomain findUserByEmail(String email) {
        return userMapper.entityToDomain(userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));
    }
}
