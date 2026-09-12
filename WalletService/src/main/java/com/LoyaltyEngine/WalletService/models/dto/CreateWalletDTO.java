package com.LoyaltyEngine.WalletService.models.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletDTO(
        @NotNull(message = "User id cant be null")
        UUID userId
) {}
