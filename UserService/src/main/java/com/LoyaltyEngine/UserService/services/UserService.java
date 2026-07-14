package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.AuthenticationException;
import com.LoyaltyEngine.UserService.exceptions.CreateUserException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
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
            UserDomain newUser = UserDomain.createUser(userDTO.getEmail(), userDTO.getFirstName(), userDTO.getLastName(), passwordEncoder.encode(userDTO.getPassword()));
            return userMapper.entityToDomain(userRepository.save(userMapper.domainToEntity(newUser)));
        } catch (DataIntegrityViolationException e) {
            log.error("Email {} already registered", userDTO.getEmail());
            throw new CreateUserException(String.format("Email [%s] already exists", userDTO.getEmail()));
        } catch (Exception e) {
            log.error("Unexpected error creating user: {}", e.getMessage());
            throw new CreateUserException(String.format("Error creating user: %s", e.getMessage()));
        }
    }

    @Transactional
    public void deleteUser(String email) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email))));
        userRepository.delete(user);
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
    public void updateUser(String email, UpdateUserDTO updateUserDTO) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email))));

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
            user.setPassword(passwordEncoder.encode(updateUserDTO.getPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
    }

    public UserDomain findUserByEmail(String email) {
        return userMapper.entityToDomain(userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email)))));
    }

    public String authUser(AuthUserDto userDto) {
        String email = userDto.getEmail();
        String password = userDto.getPassword();

        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() -> new UserNotFoundException((String.format("User by email [%s] not found", email))));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthenticationException("Password is incorrect");
        }
        return jwtService.generateJwtToken(user);
    }
}
