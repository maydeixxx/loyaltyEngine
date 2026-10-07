package com.LoyaltyEngine.ProductService.models.dtos;

import com.LoyaltyEngine.ProductService.models.domain.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProductDTO(
        UUID productId,
        UUID userId,
        String title,
        String description,
        BigDecimal price,
        ProductStatus status,
        LocalDateTime createdAt
) {
}
