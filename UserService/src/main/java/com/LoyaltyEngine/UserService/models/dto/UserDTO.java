package com.LoyaltyEngine.UserService.models.dto;


import java.util.UUID;

public record UserDTO(
        UUID id,
        String email,
        String firstName,
        String lastName
) {}
