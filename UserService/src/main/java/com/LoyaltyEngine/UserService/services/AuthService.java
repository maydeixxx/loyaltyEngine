package com.LoyaltyEngine.UserService.services;

import com.LoyaltyEngine.UserService.exceptions.AuthenticationException;
import com.LoyaltyEngine.UserService.exceptions.UserNotFoundException;
import com.LoyaltyEngine.UserService.models.User;
import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.services.interfaces.UserRepository;
import com.LoyaltyEngine.UserService.services.security.JwtService;
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
