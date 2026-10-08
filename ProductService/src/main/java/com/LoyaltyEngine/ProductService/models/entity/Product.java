package com.LoyaltyEngine.ProductService.models.entity;

import com.LoyaltyEngine.ProductService.models.domain.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "products", indexes = @Index(name = "idx_product_category", columnList = "category"))
@Getter
@Setter
@NoArgsConstructor
public class Product {
    @Id
    private UUID productId;
    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String description;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private ProductStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Product product)) return false;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }
}
