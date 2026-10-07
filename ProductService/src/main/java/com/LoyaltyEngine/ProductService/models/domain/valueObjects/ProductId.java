package com.LoyaltyEngine.ProductService.models.domain.valueObjects;

import com.github.f4b6a3.uuid.UuidCreator;

import java.util.Objects;
import java.util.UUID;

public record ProductId(UUID value) {
    public static ProductId generateProductId() {
        return new ProductId(UuidCreator.getTimeOrderedEpoch());
    }

    public ProductId {
        Objects.requireNonNull(value, "Product id cant be null");
    }
}
