package com.LoyaltyEngine.ProductService.models.domain;

import com.LoyaltyEngine.ProductService.models.domain.valueObjects.ProductId;
import com.LoyaltyEngine.ProductService.models.domain.valueObjects.UserId;
import com.LoyaltyEngine.ProductService.models.domain.enums.ProductStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Getter
public class ProductDomain {
    private final ProductId productId;
    private final UserId userId;

    private String title;
    private String description;
    private ProductStatus status;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private ProductDomain(ProductId productId, UserId userId, String title, String description, ProductStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Description is required");

        if (status == null) throw new IllegalArgumentException("Status cant be null");

        if (createdAt == null || createdAt.isBefore(LocalDateTime.now())) throw new IllegalArgumentException("Entered wrong timestamp");

        this.productId = productId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ProductDomain createProductDomain(UUID rawUserId, String title, String description) {
        ProductId productId = ProductId.generateProductId();
        UserId userId = new UserId(rawUserId);

        return new ProductDomain(productId, userId, title, description, ProductStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
    }

    public static ProductDomain restoreFromExisting(UUID rawProductId, UUID rawUserId, String title, String description, ProductStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        ProductId productId = new ProductId(rawProductId);
        UserId userId = new UserId(rawUserId);

        return new ProductDomain(productId, userId, title, description, status, createdAt, updatedAt);
    }

    public void updateTitle(String newTitle) {
        if (this.status.equals(ProductStatus.STOPPED)) throw new IllegalStateException("You cant change title for stopped listing");
        if (this.title.equals(newTitle)) throw new IllegalArgumentException("You cant enter the same title");

        this.title = newTitle;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDescription(String newDescription) {
        if (this.status.equals(ProductStatus.STOPPED)) throw new IllegalStateException("You cant change description for stopped listing");
        if (this.description.equals(newDescription)) throw new IllegalArgumentException("You cant enter the same description");

        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }

    public void stopProduct() {
        if (this.status.equals(ProductStatus.STOPPED)) throw new IllegalStateException("Product is already stopped");

        this.status = ProductStatus.STOPPED;
        this.updatedAt = LocalDateTime.now();
    }

    public void activateProduct() {
        if (this.status.equals(ProductStatus.ACTIVE)) throw new IllegalStateException("Product is already active");

        this.status = ProductStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProductDomain that)) return false;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }
}
