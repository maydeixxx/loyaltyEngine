package com.LoyaltyEngine.ProductService.services.interfaces;

import com.LoyaltyEngine.ProductService.models.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findProductByProductId(UUID productId);

    List<Product> findProductsByCategory(String category);
}
