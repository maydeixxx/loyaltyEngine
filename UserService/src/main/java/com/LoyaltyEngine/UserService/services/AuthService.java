package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.AuthenticationException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.exceptions.UserUpdateException;
import com.LoyaltyEngine.UserService.models.entity.User;
import com.LoyaltyEngine.UserService.models.domain.UserDomain;
import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.UpdateEmailDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdatePasswordDTO;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.LoyaltyEngine.UserService.services.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public String login(AuthUserDto userDto) {
        String email = userDto.email();
        String password = userDto.password();

        User user = userRepository.findUserByEmail(email)
                .filter(u -> passwordEncoder.matches(password, u.getHashedPassword()))
                .orElseThrow(() -> new AuthenticationException("Incorrect email or password"));

        return jwtService.generateJwtToken(user);
    }

    @Transactional
    public void updateUserPassword(String email, UpdatePasswordDTO updatePasswordDTO) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User by email [%s] not found".formatted(email)));

        if (!passwordEncoder.matches(updatePasswordDTO.oldPassword(), user.getHashedPassword()))
            throw new UserUpdateException("Old password is incorrect");
        if (passwordEncoder.matches(updatePasswordDTO.newPassword(), user.getHashedPassword()))
            throw new UserUpdateException("New password cant be same as old");

        UserDomain domain = userMapper.entityToDomain(user);
        domain.updatePassword(passwordEncoder.encode(updatePasswordDTO.newPassword()));

        userRepository.save(userMapper.domainToEntity(domain));
    }

    @Transactional
    public String updateEmail(String email, UpdateEmailDTO updateEmailDTO) {
        User user = userRepository.findUserByEmail(email).orElseThrow(() -> new UserNotFoundException("User by email [%s] not found".formatted(email)));
        UserDomain domain = userMapper.entityToDomain(user);

        if (userRepository.findUserByEmail(updateEmailDTO.newEmail()).isPresent())
            throw new UserUpdateException("Account with email [%s] already exists".formatted(updateEmailDTO.newEmail()));
        domain.updateEmail(updateEmailDTO.newEmail());
        user = userMapper.domainToEntity(domain);
        userRepository.save(user);

        return jwtService.generateJwtToken(user);
    }
}
