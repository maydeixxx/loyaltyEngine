package com.LoyaltyEngine.ProductService.models.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeProductStatusDTO(
        @NotNull(message = "Please provide user id")
        UUID userId,
        @NotNull(message = "Please provide product id")
        UUID productId
) {
}
