package com.LoyaltyEngine.ProductService.models;

import com.LoyaltyEngine.ProductService.models.domain.ProductDomain;
import com.LoyaltyEngine.ProductService.models.domain.enums.ProductStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductDomainTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PRODUCT_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Nested
    @DisplayName("createProductDomain scenarios")
    class CreateProductDomainTests {

        @Test
        void createProductDomain_validData_createsActiveProduct() {
            //when
            ProductDomain domain = ProductDomain.createProductDomain(
                    USER_ID,
                    "iPhone 15",
                    "Brand new smartphone",
                    "Electronics",
                    new BigDecimal("799.99")
            );

            //then
            assertAll(
                    () -> assertNotNull(domain.getProductId()),
                    () -> assertEquals(USER_ID, domain.getUserId().value()),
                    () -> assertEquals("iPhone 15", domain.getTitle()),
                    () -> assertEquals("Brand new smartphone", domain.getDescription()),
                    () -> assertEquals("Electronics", domain.getCategory()),
                    () -> assertEquals(new BigDecimal("799.99"), domain.getPrice().value()),
                    () -> assertEquals(ProductStatus.ACTIVE, domain.getStatus()),
                    () -> assertNotNull(domain.getCreatedAt()),
                    () -> assertNotNull(domain.getUpdatedAt())
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void createProductDomain_invalidTitle_throwsIllegalArgumentException(String invalidTitle) {
            assertThrows(IllegalArgumentException.class, () ->
                    ProductDomain.createProductDomain(USER_ID, invalidTitle, "Desc", "Category", BigDecimal.TEN));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void createProductDomain_invalidDescription_throwsIllegalArgumentException(String invalidDesc) {
            assertThrows(IllegalArgumentException.class, () ->
                    ProductDomain.createProductDomain(USER_ID, "Title", invalidDesc, "Category", BigDecimal.TEN));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        void createProductDomain_invalidCategory_throwsIllegalArgumentException(String invalidCategory) {
            assertThrows(IllegalArgumentException.class, () ->
                    ProductDomain.createProductDomain(USER_ID, "Title", "Desc", invalidCategory, BigDecimal.TEN));
        }

        @Test
        void createProductDomain_negativePrice_throwsIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class, () ->
                    ProductDomain.createProductDomain(USER_ID, "Title", "Desc", "Category", new BigDecimal("-1.00")));
        }
    }

    @Nested
    @DisplayName("updateTitle scenarios")
    class UpdateTitleTests {

        @Test
        void updateTitle_newTitle_updatesTitleAndTimestamp() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.updateTitle("Updated Title");

            //then
            assertEquals("Updated Title", domain.getTitle());
        }

        @Test
        void updateTitle_sameTitle_doesNotChangeOrThrow() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.updateTitle("Original Title");

            //then
            assertEquals("Original Title", domain.getTitle());
        }

        @Test
        void updateTitle_stoppedProduct_doesNotChangeOrThrow() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.STOPPED);

            //when
            domain.updateTitle("Updated Title");

            //then
            assertEquals("Original Title", domain.getTitle());
        }
    }

    @Nested
    @DisplayName("updateDescription scenarios")
    class UpdateDescriptionTests {

        @Test
        void updateDescription_newDescription_updatesDescriptionAndTimestamp() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.updateDescription("Updated Description");

            //then
            assertEquals("Updated Description", domain.getDescription());
        }

        @Test
        void updateDescription_sameDescription_doesNotChangeOrThrow() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.updateDescription("Original Description");

            //then
            assertEquals("Original Description", domain.getDescription());
        }

        @Test
        void updateDescription_stoppedProduct_doesNotChangeOrThrow() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.STOPPED);

            //when
            domain.updateDescription("Updated Description");

            //then
            assertEquals("Original Description", domain.getDescription());
        }
    }

    @Nested
    @DisplayName("updatePrice scenarios")
    class UpdatePriceTests {

        @Test
        void updatePrice_newPrice_updatesPrice() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.updatePrice(new BigDecimal("149.99"));

            //then
            assertEquals(new BigDecimal("149.99"), domain.getPrice().value());
        }

        @Test
        void updatePrice_samePrice_doesNotChangeOrThrow() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.updatePrice(new BigDecimal("99.99"));

            //then
            assertEquals(new BigDecimal("99.99"), domain.getPrice().value());
        }

        @Test
        void updatePrice_negativePrice_throwsIllegalArgumentException() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when & then
            assertThrows(IllegalArgumentException.class, () -> domain.updatePrice(new BigDecimal("-5.00")));
        }
    }

    @Nested
    @DisplayName("stopProduct & activateProduct scenarios")
    class StatusChangeTests {

        @Test
        void stopProduct_activeProduct_changesStatusToStopped() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when
            domain.stopProduct();

            //then
            assertEquals(ProductStatus.STOPPED, domain.getStatus());
        }

        @Test
        void stopProduct_alreadyStopped_throwsIllegalStateException() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.STOPPED);

            //when & then
            assertThrows(IllegalStateException.class, domain::stopProduct);
        }

        @Test
        void activateProduct_stoppedProduct_changesStatusToActive() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.STOPPED);

            //when
            domain.activateProduct();

            //then
            assertEquals(ProductStatus.ACTIVE, domain.getStatus());
        }

        @Test
        void activateProduct_alreadyActive_throwsIllegalStateException() {
            //given
            ProductDomain domain = buildDomain(ProductStatus.ACTIVE);

            //when & then
            assertThrows(IllegalStateException.class, domain::activateProduct);
        }
    }

    private ProductDomain buildDomain(ProductStatus status) {
        return ProductDomain.restoreFromExisting(
                PRODUCT_ID,
                USER_ID,
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
