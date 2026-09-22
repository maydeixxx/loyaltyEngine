package com.LoyaltyEngine.UserService.api;

import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UserDTO;
import com.LoyaltyEngine.UserService.models.enums.Role;
import com.LoyaltyEngine.UserService.services.UserService;
import com.LoyaltyEngine.UserService.services.interfaces.UserMapper;
import com.LoyaltyEngine.UserService.services.security.UserSecurity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody @Valid CreateUserDTO userDTO) {
        UserDTO createdUser = userMapper.domainToDto(userService.createUser(userDTO));
        return ResponseEntity.status(201).body(createdUser);
    }

    @PostMapping("/auth")
    public ResponseEntity<String> authenticate(@RequestBody @Valid AuthUserDto userDto) {
        String jwtToken = userService.login(userDto);
        return ResponseEntity.ok(jwtToken);
    }

    @GetMapping("/get_all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> usersList = userService.getAllUsers()
                .stream()
                .map(userMapper::domainToDto)
                .toList();

        return ResponseEntity.ok(usersList);
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable UUID id) {
        UserDTO user = userMapper.domainToDto(userService.findUserById(id));
        return ResponseEntity.ok(user);
    }

    @GetMapping()
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getSelf(@AuthenticationPrincipal UserSecurity userSecurity) {
        UserDTO user = userMapper.domainToDto(userService.findUserByEmail(userSecurity.email()));
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.email == #email")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        userService.deleteUser(email);
        return ResponseEntity.status(204).build();
    }

    @PutMapping("/{email}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.email == #email")
    public ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody @Valid UpdateUserDTO userDTO) {
        userService.updateUser(email, userDTO);
        return ResponseEntity.status(204).build();
    }
}
