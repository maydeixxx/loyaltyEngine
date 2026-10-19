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
import com.LoyaltyEngine.ProductService.services.security.UserSecurity;
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
        UUID userId = dto.userId();
        String title = dto.title();
        String description = dto.description();
        BigDecimal price = dto.price();
        String category = dto.category();

        ProductDomain productDomain = ProductDomain.createProductDomain(userId, title, description, category, price);
        productRepository.save(productMapper.domainToEntity(productDomain));
    }

    @Transactional
    public void deleteProduct(UUID productId, UserSecurity userSecurity) {
        Product product = productRepository.findProductByProductId(productId).orElseThrow(() -> new ProductNotFoundException("Product by id [%s] not found".formatted(productId)));
        UUID ownerUserId = product.getUserId();
        if (!userSecurity.userId().equals(ownerUserId) && !userSecurity.isAdmin())
            throw new ProductDeletingException("You are trying to delete not your product");

        productRepository.delete(product);
    }

    @Transactional
    public void updateProduct(UUID productId, UpdateProductDTO dto, UserSecurity userSecurity) {
        String description = dto.description();
        String title = dto.title();
        BigDecimal price = dto.price();
        ProductDomain product = findProductById(productId);
        if (!userSecurity.userId().equals(product.getUserId().value()) && !userSecurity.isAdmin())
            throw new ProductDeletingException("You are trying to delete not your product");

        if (title != null && !title.isBlank()) product.updateTitle(title);
        if (description != null && !description.isBlank()) product.updateDescription(description);
        if (price != null) product.updatePrice(price);

        productRepository.save(productMapper.domainToEntity(product));
    }

    @Transactional
    public void stopProduct(ChangeProductStatusDTO dto, UserSecurity userSecurity) {
        ProductDomain product = findProductById(dto.productId());
        if (!userSecurity.userId().equals(product.getUserId().value()) && !userSecurity.isAdmin())
            throw new ProductDeletingException("You are trying to delete not your product");

        product.stopProduct();

        productRepository.save(productMapper.domainToEntity(product));
    }

    @Transactional
    public void activate(ChangeProductStatusDTO dto, UserSecurity userSecurity) {
        ProductDomain product = findProductById(dto.productId());
        if (!userSecurity.userId().equals(product.getUserId().value()) && !userSecurity.isAdmin())
            throw new ProductDeletingException("You are trying to delete not your product");

        product.activateProduct();

        productRepository.save(productMapper.domainToEntity(product));
    }

    public List<ProductDTO> findAllProducts() {
        return productRepository.findAll().stream()
                .map(productMapper::entityToDto)
                .toList();
    }

    public List<ProductDTO> findProductsByCategory(String category) {
        return productRepository.findProductsByCategory(category).stream()
                .map(productMapper::entityToDto)
                .toList();
    }

    public ProductDomain findProductById(UUID productId) {
        if (productId == null) throw new NullPointerException("Please provide correct product id");
        return productMapper.entityToDomain(productRepository.findProductByProductId(productId).orElseThrow(() -> new ProductNotFoundException("Product by id [%s] not found".formatted(productId))));
    }
}
