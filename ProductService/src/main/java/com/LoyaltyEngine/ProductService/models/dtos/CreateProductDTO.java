package com.LoyaltyEngine.ProductService.models.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

public record CreateProductDTO(
        @NotNull(message = "User id cant be null")
        UUID userId,
        @NotBlank(message = "Title cant be empty or null")
        @Length(min = 10, max = 50, message = "Length of title is between 10 and 50")
        String title,
        @Length(min = 10, max = 200, message = "Description of title is between 10 and 50")
        String description
) {
}
