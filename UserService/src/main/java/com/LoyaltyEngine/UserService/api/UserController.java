package com.LoyaltyEngine.UserService.api;

import com.LoyaltyEngine.UserService.models.dto.AuthUserDto;
import com.LoyaltyEngine.UserService.models.dto.CreateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UpdateUserDTO;
import com.LoyaltyEngine.UserService.models.dto.UserDTO;
import com.LoyaltyEngine.UserService.services.UserService;
import com.LoyaltyEngine.UserService.services.interfaces.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<UserDTO> registerUser(@RequestBody CreateUserDTO userDTO) {
        UserDTO createdUser = userMapper.domainToDto(userService.createUser(userDTO));
        return ResponseEntity.status(201).body(createdUser);
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

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userMapper.domainToDto(userService.findUserById(id));
        return ResponseEntity.ok(user);
    }

    @GetMapping()
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserDTO> getSelf() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        UserDTO user = userMapper.domainToDto(userService.findUserByEmail(email));
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{email}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteUser(@PathVariable String email) {
        String authenticatedEmail = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        boolean hasRoleAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(Object::toString)
                .toList()
                .contains("ROLE_ADMIN");

        if (authenticatedEmail.equals(email) || hasRoleAdmin) {
            userService.deleteUser(email);
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.badRequest().body("You cant delete this user");
        }
    }

    @PutMapping("/update/{email}")
    public ResponseEntity<?> updateUser(@PathVariable String email, @RequestBody UpdateUserDTO userDTO) {
        String authenticatedEmail = SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal()
                .toString();

        boolean hasRoleAdmin = SecurityContextHolder.getContext()
                .getAuthentication()
                .getAuthorities()
                .stream()
                .map(Object::toString)
                .toList()
                .contains("ROLE_ADMIN");

        if (authenticatedEmail.equals(email) || hasRoleAdmin) {
            userService.updateUser(email, userDTO);
            return ResponseEntity.status(204).build();
        } else {
            return ResponseEntity.badRequest().body("You cant update this user");
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<String> authenticate(@RequestBody AuthUserDto userDto) {
        String jwtToken = userService.authUser(userDto);
        return ResponseEntity.ok(jwtToken);
    }
}
