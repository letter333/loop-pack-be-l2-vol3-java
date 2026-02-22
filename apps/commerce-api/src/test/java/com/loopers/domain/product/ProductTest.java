package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Product 도메인 단위 테스트")
class ProductTest {

    @Nested
    @DisplayName("Product 생성")
    class Create {

        @Test
        @DisplayName("모든 필수값이 유효하면 정상적으로 생성된다")
        void createsProduct_whenAllRequiredFieldsAreValid() {
            // Arrange & Act
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Assert
            assertAll(
                () -> assertThat(product.getName()).isEqualTo("아이폰 15"),
                () -> assertThat(product.getBrandId()).isEqualTo(1L),
                () -> assertThat(product.getCategoryId()).isEqualTo(1L),
                () -> assertThat(product.getBasePrice()).isEqualTo(1500000L),
                () -> assertThat(product.getStatus()).isEqualTo(ProductStatus.SALE),
                () -> assertThat(product.isDeleted()).isFalse()
            );
        }

        @Test
        @DisplayName("productCode가 자동 생성된다 (YYYYMMDD-5자리)")
        void generatesProductCode_whenCreated() {
            // Arrange & Act
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Assert
            assertThat(product.getProductCode()).isNotNull();
            assertThat(product.getProductCode()).matches("\\d{8}-\\d{5}");
        }

        @Test
        @DisplayName("name이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Product(null, 1L, 1L, 1500000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsEmpty() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Product("", 1L, 1L, 1500000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("brandId가 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenBrandIdIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Product("아이폰 15", null, 1L, 1500000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("categoryId가 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenCategoryIdIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Product("아이폰 15", 1L, null, 1500000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("basePrice가 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenBasePriceIsNull() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Product("아이폰 15", 1L, 1L, null))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("basePrice가 0 미만이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenBasePriceIsNegative() {
            // Arrange & Act & Assert
            assertThatThrownBy(() -> new Product("아이폰 15", 1L, 1L, -1L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("basePrice가 0이면 정상적으로 생성된다 (무료 상품)")
        void createsProduct_whenBasePriceIsZero() {
            // Arrange & Act
            Product product = new Product("무료 상품", 1L, 1L, 0L);

            // Assert
            assertThat(product.getBasePrice()).isEqualTo(0L);
        }
    }
}