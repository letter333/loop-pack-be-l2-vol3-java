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

    @Nested
    @DisplayName("calculateDiscountedPrice - 할인가 계산")
    class CalculateDiscountedPrice {

        @Test
        @DisplayName("할인이 없으면 기본가를 반환한다")
        void returnsBasePrice_whenNoDiscount() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act
            Long discountedPrice = product.calculateDiscountedPrice();

            // Assert
            assertThat(discountedPrice).isEqualTo(1500000L);
        }

        @Test
        @DisplayName("PRICE 타입: 정액 할인을 적용한다")
        void appliesPriceDiscount() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            product.applyDiscount(100000L, DiscountType.PRICE);

            // Act
            Long discountedPrice = product.calculateDiscountedPrice();

            // Assert
            assertThat(discountedPrice).isEqualTo(1400000L);
        }

        @Test
        @DisplayName("RATE 타입: 정률 할인을 적용한다 (10% 할인)")
        void appliesRateDiscount() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1000000L);
            product.applyDiscount(10L, DiscountType.RATE);

            // Act
            Long discountedPrice = product.calculateDiscountedPrice();

            // Assert
            assertThat(discountedPrice).isEqualTo(900000L);
        }

        @Test
        @DisplayName("PRICE 타입: 할인가가 기본가보다 크면 0원을 반환한다")
        void returnsZero_whenPriceDiscountExceedsBasePrice() {
            // Arrange
            Product product = new Product("저가 상품", 1L, 1L, 50000L);
            product.applyDiscount(100000L, DiscountType.PRICE);

            // Act
            Long discountedPrice = product.calculateDiscountedPrice();

            // Assert
            assertThat(discountedPrice).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("applyDiscount - 할인 적용")
    class ApplyDiscount {

        @Test
        @DisplayName("RATE 타입에서 discount가 100 초과이면 예외가 발생한다")
        void throwsException_whenRateDiscountExceeds100() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act & Assert
            assertThatThrownBy(() -> product.applyDiscount(101L, DiscountType.RATE))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("할인을 제거할 수 있다")
        void removesDiscount() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            product.applyDiscount(100000L, DiscountType.PRICE);

            // Act
            product.removeDiscount();

            // Assert
            assertAll(
                () -> assertThat(product.getDiscount()).isNull(),
                () -> assertThat(product.getDiscountType()).isNull(),
                () -> assertThat(product.calculateDiscountedPrice()).isEqualTo(1500000L)
            );
        }
    }
}