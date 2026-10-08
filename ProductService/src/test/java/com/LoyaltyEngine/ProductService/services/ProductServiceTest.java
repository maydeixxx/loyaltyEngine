package com.LoyaltyEngine.ProductService.services;

import com.LoyaltyEngine.ProductService.exceptions.ProductDeletingException;
import com.LoyaltyEngine.ProductService.exceptions.ProductNotFoundException;
import com.LoyaltyEngine.ProductService.models.domain.ProductDomain;
import com.LoyaltyEngine.ProductService.models.domain.enums.ProductStatus;
import com.LoyaltyEngine.ProductService.models.dtos.ChangeProductStatusDTO;
import com.LoyaltyEngine.ProductService.models.dtos.CreateProductDTO;
import com.LoyaltyEngine.ProductService.models.dtos.ProductDTO;
import com.LoyaltyEngine.ProductService.models.dtos.UpdateProductDTO;
import com.LoyaltyEngine.ProductService.models.entity.Product;
import com.LoyaltyEngine.ProductService.services.interfaces.ProductRepository;
import com.LoyaltyEngine.ProductService.services.security.UserSecurity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OWNER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID ADMIN_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private static final UserSecurity OWNER_SECURITY = new UserSecurity(OWNER_USER_ID, "USER");
    private static final UserSecurity OTHER_SECURITY = new UserSecurity(OTHER_USER_ID, "USER");
    private static final UserSecurity ADMIN_SECURITY = new UserSecurity(ADMIN_USER_ID, "ROLE_ADMIN");

    @Nested
    @DisplayName("createProduct scenarios")
    class CreateProductTests {

        @Test
        void createProduct_validDto_savesProduct() {
            //given
            CreateProductDTO dto = new CreateProductDTO(
                    OWNER_USER_ID,
                    "Smartphone",
                    "Latest flagship",
                    "Electronics",
                    new BigDecimal("999.99")
            );
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            when(productMapper.domainToEntity(any(ProductDomain.class))).thenReturn(entity);

            //when
            productService.createProduct(dto);

            //then
            verify(productRepository).save(entity);
        }

        @Test
        void createProduct_invalidPrice_throwsIllegalArgumentException() {
            //given
            CreateProductDTO dto = new CreateProductDTO(
                    OWNER_USER_ID,
                    "Smartphone",
                    "Latest flagship",
                    "Electronics",
                    new BigDecimal("-10.00")
            );

            //when & then
            assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
            verify(productRepository, never()).save(any());
        }

        @Test
        void createProduct_blankTitle_throwsIllegalArgumentException() {
            //given
            CreateProductDTO dto = new CreateProductDTO(
                    OWNER_USER_ID,
                    "  ",
                    "Description",
                    "Category",
                    new BigDecimal("100.00")
            );

            //when & then
            assertThrows(IllegalArgumentException.class, () -> productService.createProduct(dto));
            verify(productRepository, never()).save(any());
        }

        @Test
        void createProduct_repositoryFails_throwsRuntimeException() {
            //given
            CreateProductDTO dto = new CreateProductDTO(
                    OWNER_USER_ID,
                    "Smartphone",
                    "Latest flagship",
                    "Electronics",
                    new BigDecimal("999.99")
            );
            when(productMapper.domainToEntity(any(ProductDomain.class))).thenThrow(new IllegalStateException("Mapping error"));

            //when & then
            assertThrows(RuntimeException.class, () -> productService.createProduct(dto));
        }
    }

    @Nested
    @DisplayName("deleteProduct scenarios")
    class DeleteProductTests {

        @Test
        void deleteProduct_existingProductAndOwner_deletesProduct() {
            //given
            Product product = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

            //when
            productService.deleteProduct(PRODUCT_ID, OWNER_SECURITY);

            //then
            verify(productRepository).delete(product);
        }

        @Test
        void deleteProduct_existingProductAndAdmin_deletesProduct() {
            //given
            Product product = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

            //when
            productService.deleteProduct(PRODUCT_ID, ADMIN_SECURITY);

            //then
            verify(productRepository).delete(product);
        }

        @Test
        void deleteProduct_notOwnerAndNotAdmin_throwsProductDeletingException() {
            //given
            Product product = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(product));

            //when & then
            assertThrows(ProductDeletingException.class, () -> productService.deleteProduct(PRODUCT_ID, OTHER_SECURITY));
            verify(productRepository, never()).delete(any());
        }

        @Test
        void deleteProduct_productNotFound_throwsProductNotFoundException() {
            //given
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

            //when & then
            assertThrows(ProductNotFoundException.class, () -> productService.deleteProduct(PRODUCT_ID, OWNER_SECURITY));
            verify(productRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("updateProduct scenarios")
    class UpdateProductTests {

        @Test
        void updateProduct_validDtoAndOwner_updatesAndSavesProduct() {
            //given
            UpdateProductDTO dto = new UpdateProductDTO(OWNER_USER_ID, "New Title", "New Desc", new BigDecimal("199.99"));
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.updateProduct(PRODUCT_ID, dto, OWNER_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertEquals("New Title", domain.getTitle());
            assertEquals("New Desc", domain.getDescription());
            assertEquals(new BigDecimal("199.99"), domain.getPrice().value());
        }

        @Test
        void updateProduct_validDtoAndAdmin_updatesAndSavesProduct() {
            //given
            UpdateProductDTO dto = new UpdateProductDTO(ADMIN_USER_ID, "Admin Updated Title", null, null);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.updateProduct(PRODUCT_ID, dto, ADMIN_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertEquals("Admin Updated Title", domain.getTitle());
        }

        @Test
        void updateProduct_notOwnerAndNotAdmin_throwsProductDeletingException() {
            //given
            UpdateProductDTO dto = new UpdateProductDTO(OTHER_USER_ID, "Hacked Title", null, null);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);

            //when & then
            assertThrows(ProductDeletingException.class, () -> productService.updateProduct(PRODUCT_ID, dto, OTHER_SECURITY));
            verify(productRepository, never()).save(any());
        }

        @Test
        void updateProduct_partialUpdateOnlyPrice_updatesOnlyPrice() {
            //given
            UpdateProductDTO dto = new UpdateProductDTO(OWNER_USER_ID, null, null, new BigDecimal("350.00"));
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.updateProduct(PRODUCT_ID, dto, OWNER_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertAll(
                    () -> assertEquals("Original Title", domain.getTitle()),
                    () -> assertEquals("Original Description", domain.getDescription()),
                    () -> assertEquals(new BigDecimal("350.00"), domain.getPrice().value())
            );
        }

        @Test
        void updateProduct_productNotFound_throwsProductNotFoundException() {
            //given
            UpdateProductDTO dto = new UpdateProductDTO(OWNER_USER_ID, "New Title", null, null);
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

            //when & then
            assertThrows(ProductNotFoundException.class, () -> productService.updateProduct(PRODUCT_ID, dto, OWNER_SECURITY));
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("stopProduct scenarios")
    class StopProductTests {

        @Test
        void stopProduct_activeProductAndOwner_stopsAndSavesProduct() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OWNER_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.stopProduct(dto, OWNER_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertEquals(ProductStatus.STOPPED, domain.getStatus());
        }

        @Test
        void stopProduct_activeProductAndAdmin_stopsAndSavesProduct() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(ADMIN_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.stopProduct(dto, ADMIN_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertEquals(ProductStatus.STOPPED, domain.getStatus());
        }

        @Test
        void stopProduct_notOwnerAndNotAdmin_throwsProductDeletingException() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OTHER_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);

            //when & then
            assertThrows(ProductDeletingException.class, () -> productService.stopProduct(dto, OTHER_SECURITY));
            verify(productRepository, never()).save(any());
        }

        @Test
        void stopProduct_alreadyStopped_throwsIllegalStateException() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OWNER_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);

            //when & then
            assertThrows(IllegalStateException.class, () -> productService.stopProduct(dto, OWNER_SECURITY));
            verify(productRepository, never()).save(any());
        }

        @Test
        void stopProduct_productNotFound_throwsProductNotFoundException() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OWNER_USER_ID, PRODUCT_ID);
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

            //when & then
            assertThrows(ProductNotFoundException.class, () -> productService.stopProduct(dto, OWNER_SECURITY));
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("activate scenarios")
    class ActivateTests {

        @Test
        void activate_stoppedProductAndOwner_activatesAndSavesProduct() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OWNER_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.activate(dto, OWNER_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertEquals(ProductStatus.ACTIVE, domain.getStatus());
        }

        @Test
        void activate_stoppedProductAndAdmin_activatesAndSavesProduct() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(ADMIN_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);
            when(productMapper.domainToEntity(domain)).thenReturn(entity);

            //when
            productService.activate(dto, ADMIN_SECURITY);

            //then
            verify(productRepository).save(entity);
            assertEquals(ProductStatus.ACTIVE, domain.getStatus());
        }

        @Test
        void activate_notOwnerAndNotAdmin_throwsProductDeletingException() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OTHER_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.STOPPED);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);

            //when & then
            assertThrows(ProductDeletingException.class, () -> productService.activate(dto, OTHER_SECURITY));
            verify(productRepository, never()).save(any());
        }

        @Test
        void activate_alreadyActive_throwsIllegalStateException() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OWNER_USER_ID, PRODUCT_ID);
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);

            //when & then
            assertThrows(IllegalStateException.class, () -> productService.activate(dto, OWNER_SECURITY));
            verify(productRepository, never()).save(any());
        }

        @Test
        void activate_productNotFound_throwsProductNotFoundException() {
            //given
            ChangeProductStatusDTO dto = new ChangeProductStatusDTO(OWNER_USER_ID, PRODUCT_ID);
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

            //when & then
            assertThrows(ProductNotFoundException.class, () -> productService.activate(dto, OWNER_SECURITY));
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("findProductsByCategory scenarios")
    class FindProductsByCategoryTests {

        @Test
        void findProductsByCategory_existingCategory_returnsProductDtoList() {
            //given
            String category = "Electronics";
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDTO dto = new ProductDTO(PRODUCT_ID, OWNER_USER_ID, "Title", "Desc", category, new BigDecimal("99.99"), ProductStatus.ACTIVE, LocalDateTime.now().minusDays(1));

            when(productRepository.findProductsByCategory(category)).thenReturn(List.of(entity));
            when(productMapper.entityToDto(entity)).thenReturn(dto);

            //when
            List<ProductDTO> result = productService.findProductsByCategory(category);

            //then
            assertAll(
                    () -> assertEquals(1, result.size()),
                    () -> assertEquals(PRODUCT_ID, result.getFirst().productId()),
                    () -> assertEquals(category, result.getFirst().category())
            );
        }

        @Test
        void findProductsByCategory_emptyList_returnsEmptyList() {
            //given
            when(productRepository.findProductsByCategory("Unknown")).thenReturn(List.of());

            //when
            List<ProductDTO> result = productService.findProductsByCategory("Unknown");

            //then
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findProductById scenarios")
    class FindProductByIdTests {

        @Test
        void findProductById_existingId_returnsProductDomain() {
            //given
            Product entity = buildProductEntity(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);
            ProductDomain domain = buildProductDomain(PRODUCT_ID, OWNER_USER_ID, ProductStatus.ACTIVE);

            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.of(entity));
            when(productMapper.entityToDomain(entity)).thenReturn(domain);

            //when
            ProductDomain result = productService.findProductById(PRODUCT_ID);

            //then
            assertNotNull(result);
            assertEquals(PRODUCT_ID, result.getProductId().value());
        }

        @Test
        void findProductById_missingId_throwsProductNotFoundException() {
            //given
            when(productRepository.findProductByProductId(PRODUCT_ID)).thenReturn(Optional.empty());

            //when & then
            assertThrows(ProductNotFoundException.class, () -> productService.findProductById(PRODUCT_ID));
        }

        @Test
        void findProductById_nullId_throwsNullPointerException() {
            //when & then
            assertThrows(NullPointerException.class, () -> productService.findProductById(null));
        }
    }

    // Helper factory methods
    private Product buildProductEntity(UUID productId, UUID userId, ProductStatus status) {
        Product product = new Product();
        product.setProductId(productId);
        product.setUserId(userId);
        product.setTitle("Original Title");
        product.setDescription("Original Description");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("99.99"));
        product.setStatus(status);
        product.setCreatedAt(LocalDateTime.now().minusDays(1));
        product.setUpdatedAt(LocalDateTime.now().minusDays(1));
        return product;
    }

    private ProductDomain buildProductDomain(UUID productId, UUID userId, ProductStatus status) {
        return ProductDomain.restoreFromExisting(
                productId,
                userId,
                "Original Title",
                "Original Description",
                "Electronics",
                new BigDecimal("99.99"),
                status,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(1)
        );
    }
}
