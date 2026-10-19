package com.LoyaltyEngine.ProductService.services;

import com.LoyaltyEngine.ProductService.models.domain.ProductDomain;
import com.LoyaltyEngine.ProductService.models.dtos.ProductDTO;
import com.LoyaltyEngine.ProductService.models.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {
    public ProductDomain entityToDomain(Product entity) {
        return ProductDomain.restoreFromExisting(
                entity.getProductId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public Product domainToEntity(ProductDomain domain) {
        Product product = new Product();
        product.setProductId(domain.getProductId().value());
        product.setUserId(domain.getUserId().value());
        product.setTitle(domain.getTitle());
        product.setDescription(domain.getDescription());
        product.setCategory(domain.getCategory());
        product.setPrice(domain.getPrice().value());
        product.setStatus(domain.getStatus());
        product.setCreatedAt(domain.getCreatedAt());
        product.setUpdatedAt(domain.getUpdatedAt());

        return product;
    }

    public ProductDTO domainToDto(ProductDomain domain) {
        return new ProductDTO(
                domain.getProductId().value(),
                domain.getUserId().value(),
                domain.getTitle(),
                domain.getDescription(),
                domain.getCategory(),
                domain.getPrice().value(),
                domain.getStatus(),
                domain.getCreatedAt()
        );
    }

    public ProductDTO entityToDto(Product entity) {
        return new ProductDTO(
                entity.getProductId(),
                entity.getUserId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getPrice(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }
}
