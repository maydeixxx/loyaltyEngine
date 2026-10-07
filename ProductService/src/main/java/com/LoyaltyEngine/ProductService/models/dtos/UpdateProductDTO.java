package com.LoyaltyEngine.ProductService.models.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateProductDTO(
        @NotNull(message = "please, provide user id") UUID userId,
        String title,
        String description
) {
}
