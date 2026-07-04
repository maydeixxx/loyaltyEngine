package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.DeleteUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.models.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.services.interfaces.UserMapper;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public void createUser(CreateUserDTO userDTO) {
        try {
            UserDomain newUser = UserDomain.createUser(userDTO.getEmail(), userDTO.getFirstName(), userDTO.getLastName(), passwordEncoder.encode(userDTO.getPassword()));
            userRepository.save(userMapper.domainToEntity(newUser));
        } catch (Exception e) {
            log.info("CREATING USER ERROR: {}", e.getMessage());
            throw new CreateUserException(String.format("Error creating user: %s", e.getMessage()));
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        try {
            User user = userRepository.findUserById(id).orElseThrow(() -> new UserNotFoundException((String.format("User by id [%s] not found", id))));
            userRepository.delete(user);
        } catch (Exception e) {
            log.error("ERROR DELETING USER");
            throw new DeleteUserException(String.format("Error deleting user by id [%s]: %s", id, e.getMessage()));
        }
    }

    public UserDomain findUserById(Long id) {
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
    public void updateUser(UpdateUserDTO updateUserDTO) {
        Long id = updateUserDTO.getId();
        User user = userRepository.findUserById(id).orElseThrow(() -> new UserNotFoundException((String.format("User by id [%s] not found", id))));

        if (updateUserDTO.getEmail() != null) {
            user.setEmail(updateUserDTO.getEmail());
        }

        if (updateUserDTO.getFirstName() != null) {
            user.setFirstName(updateUserDTO.getFirstName());
        }

        if (updateUserDTO.getLastName() != null) {
            user.setLastName(updateUserDTO.getLastName());
        }

        if (updateUserDTO.getPassword() != null) {
            user.setPassword(updateUserDTO.getPassword());
        }

        user.setUpdatedAt(LocalDateTime.now());
    }
}
