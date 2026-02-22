package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("ProductService 통합 테스트")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("존재하는 상품을 조회하면 Product를 반환한다")
        void returnsProduct_whenProductExists() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.getProduct(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getName()).isEqualTo("아이폰 15")
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getActiveProduct")
    class GetActiveProduct {

        @Test
        @DisplayName("활성 상품을 조회하면 Product를 반환한다")
        void returnsProduct_whenProductIsActive() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.getActiveProduct(saved.getId());

            // Assert
            assertThat(result.getName()).isEqualTo("아이폰 15");
        }

        @Test
        @DisplayName("삭제된 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductIsDeleted() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productService.deleteProduct(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> productService.getActiveProduct(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.getActiveProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("상품을 정상적으로 생성한다")
        void createsProduct() {
            // Act
            Product result = productService.createProduct("아이폰 15", 1L, 1L, 1500000L);

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("아이폰 15"),
                () -> assertThat(result.getBrandId()).isEqualTo(1L),
                () -> assertThat(result.getCategoryId()).isEqualTo(1L),
                () -> assertThat(result.getBasePrice()).isEqualTo(1500000L),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.SALE),
                () -> assertThat(result.getProductCode()).matches("\\d{8}-\\d{5}")
            );
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("상품을 정상적으로 수정한다")
        void updatesProduct() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.updateProduct(
                saved.getId(), "아이폰 15 Pro", 2L, 1800000L,
                100000L, DiscountType.PRICE, ProductStatus.STOP
            );

            // Assert
            assertAll(
                () -> assertThat(result.getName()).isEqualTo("아이폰 15 Pro"),
                () -> assertThat(result.getCategoryId()).isEqualTo(2L),
                () -> assertThat(result.getBasePrice()).isEqualTo(1800000L),
                () -> assertThat(result.getDiscount()).isEqualTo(100000L),
                () -> assertThat(result.getDiscountType()).isEqualTo(DiscountType.PRICE),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.STOP)
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(
                999L, "아이폰 15 Pro", 2L, 1800000L,
                null, null, ProductStatus.SALE
            ))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("상품을 삭제하면 Soft Delete 된다")
        void deletesProduct() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            productService.deleteProduct(saved.getId());

            // Assert
            Product deleted = productService.getProduct(saved.getId());
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }
}