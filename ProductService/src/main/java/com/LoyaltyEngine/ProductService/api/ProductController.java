package com.LoyaltyEngine.ProductService.api;

import com.LoyaltyEngine.ProductService.models.dtos.ChangeProductStatusDTO;
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

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductMapper productMapper;
    private final ProductService productService;

    @PostMapping()
    @PreAuthorize("isAuthenticated() and authentication.principal.userId == #productDTO.userId()")
    public ResponseEntity<Void> createProduct(@RequestBody @Valid CreateProductDTO productDTO) {
        productService.createProduct(productDTO);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{productId}")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or authentication.principal.userId == #productDTO.userId())")
    public ResponseEntity<Void> updateProduct(@PathVariable UUID productId, @RequestBody @Valid UpdateProductDTO productDTO) {
        UserSecurity userSecurity = (UserSecurity) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        productService.updateProduct(productId, productDTO, userSecurity);
        return ResponseEntity.status(204).build();
    }

    @PutMapping("/activate")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or authentication.principal.userId == #productDTO.userId())")
    public ResponseEntity<Void> activateProduct(@RequestBody @Valid ChangeProductStatusDTO productDTO) {
        UserSecurity userSecurity = (UserSecurity) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        productService.activate(productDTO, userSecurity);
        return ResponseEntity.status(204).build();
    }

    @PutMapping("/stop")
    @PreAuthorize("isAuthenticated() and (hasRole('ADMIN') or authentication.principal.userId == #productDTO.userId())")
    public ResponseEntity<Void> stopProduct(@RequestBody @Valid ChangeProductStatusDTO productDTO) {
        UserSecurity userSecurity = (UserSecurity) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        productService.stopProduct(productDTO, userSecurity);
        return ResponseEntity.status(204).build();
    }

    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteProduct(@PathVariable UUID productId) {
        UserSecurity userSecurity = (UserSecurity) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        productService.deleteProduct(productId, userSecurity);
        return ResponseEntity.status(204).build();
    }

    @GetMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductDTO> getProduct(@PathVariable UUID productId) {
        ProductDTO product = productMapper.domainToDto(productService.findProductById(productId));
        return ResponseEntity.ok(product);
    }

    @GetMapping("/all")
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.findAllProducts());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<ProductDTO>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.findProductsByCategory(category));
    }
}
