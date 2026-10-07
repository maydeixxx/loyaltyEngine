package com.LoyaltyEngine.ProductService.api;

import com.LoyaltyEngine.ProductService.models.dtos.CreateProductDTO;
import com.LoyaltyEngine.ProductService.models.dtos.ProductDTO;
import com.LoyaltyEngine.ProductService.models.dtos.UpdateProductDTO;
import com.LoyaltyEngine.ProductService.services.ProductMapper;
import com.LoyaltyEngine.ProductService.services.ProductService;
import com.LoyaltyEngine.ProductService.services.security.UserSecurity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductMapper productMapper;
    private final ProductService productService;

    @PostMapping()
    @PreAuthorize("authentication.principal.userId == #productDTO.userId()")
    public ResponseEntity<Void> createProduct(@RequestBody @Valid CreateProductDTO productDTO) {
        productService.createProduct(productDTO);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.userId == #productDTO.userId()")
    public ResponseEntity<Void> updateProduct(@PathVariable UUID productId, @RequestBody @Valid UpdateProductDTO productDTO) {
        productService.updateProduct(productId, productDTO);
        return ResponseEntity.status(204).build();
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        UserSecurity userSecurity = (UserSecurity) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        productService.deleteProduct(productId, Objects.requireNonNull(userSecurity).userId());
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable UUID productId) {
        ProductDTO product = productMapper.domainToDto(productService.findProductById(productId));
        return ResponseEntity.ok(product);
    }
}
