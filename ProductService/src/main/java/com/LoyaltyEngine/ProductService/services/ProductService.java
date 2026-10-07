package com.LoyaltyEngine.ProductService.services;

import com.LoyaltyEngine.ProductService.exceptions.ProductDeletingException;
import com.LoyaltyEngine.ProductService.exceptions.ProductNotFoundException;
import com.LoyaltyEngine.ProductService.models.domain.ProductDomain;
import com.LoyaltyEngine.ProductService.models.dtos.ChangeProductStatusDTO;
import com.LoyaltyEngine.ProductService.models.dtos.CreateProductDTO;
import com.LoyaltyEngine.ProductService.models.dtos.ProductDTO;
import com.LoyaltyEngine.ProductService.models.dtos.UpdateProductDTO;
import com.LoyaltyEngine.ProductService.models.entity.Product;
import com.LoyaltyEngine.ProductService.services.interfaces.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductMapper productMapper;
    private final ProductRepository productRepository;

    @Transactional
    public void createProduct(CreateProductDTO dto) {
        try {
            UUID userId = dto.userId();
            String title = dto.title();
            String description = dto.description();
            BigDecimal price = dto.price();
            String category = dto.category();

            ProductDomain productDomain = ProductDomain.createProductDomain(userId, title, description, category, price);
            productRepository.save(productMapper.domainToEntity(productDomain));
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating new product: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void deleteProduct(UUID productId, UUID userId) {
        try {
            Product product = productRepository.findProductByProductId(productId).orElseThrow(() -> new ProductNotFoundException("Product by id [%s] not found".formatted(productId)));
            UUID ownerUserId = product.getUserId();
            if (!userId.equals(ownerUserId)) throw new ProductDeletingException("You are trying to delete not your product");

            productRepository.delete(product);
        } catch (ProductDeletingException | ProductNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error deleting product [{}]: {}", productId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void updateProduct(UUID productId, UpdateProductDTO dto) {
        try {
            String description = dto.description();
            String title = dto.title();
            BigDecimal price = dto.price();
            ProductDomain product = findProductById(productId);

            if (title != null && !title.isBlank()) product.updateTitle(title);
            if (description != null && !description.isBlank()) product.updateDescription(description);
            if (price != null) product.updatePrice(price);

            productRepository.save(productMapper.domainToEntity(product));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating product [{}] : {}", productId, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public void stopProduct(ChangeProductStatusDTO dto) {
        try {
            ProductDomain product = findProductById(dto.productId());
            product.stopProduct();

            productRepository.save(productMapper.domainToEntity(product));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error stopping product [{}] : {}", dto.productId(), e.getMessage());
        }
    }

    @Transactional
    public void activate(ChangeProductStatusDTO dto) {
        try {
            ProductDomain product = findProductById(dto.productId());
            product.activateProduct();

            productRepository.save(productMapper.domainToEntity(product));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error activating product [{}] : {}", dto.productId(), e.getMessage());
        }
    }

    public List<ProductDTO> findProductsByCategory(String category) {
        try {
            return productRepository.findProductsByCategory(category).stream()
                    .map(productMapper::entityToDto)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to find products by category [{}]: {}", category, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public ProductDomain findProductById(UUID productId) {
        try {
            if (productId == null) throw new NullPointerException("Please provide correct product id");
            return productMapper.entityToDomain(productRepository.findProductByProductId(productId).orElseThrow(() -> new ProductNotFoundException("Product by id [%s] not found".formatted(productId))));
        } catch (ProductNotFoundException | NullPointerException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to find product by id [{}]: {}", productId, e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
