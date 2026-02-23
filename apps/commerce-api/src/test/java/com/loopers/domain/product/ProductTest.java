package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

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

    @Nested
    @DisplayName("Product update")
    class Update {

        @Test
        @DisplayName("모든 필드를 정상적으로 업데이트한다")
        void updatesAllFields() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act
            product.update("아이폰 15 Pro", 2L, 1800000L, 50000L, DiscountType.PRICE, ProductStatus.STOP);

            // Assert
            assertAll(
                () -> assertThat(product.getName()).isEqualTo("아이폰 15 Pro"),
                () -> assertThat(product.getCategoryId()).isEqualTo(2L),
                () -> assertThat(product.getBasePrice()).isEqualTo(1800000L),
                () -> assertThat(product.getDiscount()).isEqualTo(50000L),
                () -> assertThat(product.getDiscountType()).isEqualTo(DiscountType.PRICE),
                () -> assertThat(product.getStatus()).isEqualTo(ProductStatus.STOP)
            );
        }

        @Test
        @DisplayName("brandId는 변경되지 않는다")
        void brandIdRemainsUnchanged() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            Long originalBrandId = product.getBrandId();

            // Act
            product.update("아이폰 15 Pro", 2L, 1800000L, null, null, ProductStatus.SALE);

            // Assert
            assertThat(product.getBrandId()).isEqualTo(originalBrandId);
        }

        @Test
        @DisplayName("name을 null로 업데이트하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenUpdateNameToNull() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act & Assert
            assertThatThrownBy(() -> product.update(null, 2L, 1800000L, null, null, ProductStatus.SALE))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("name을 빈 문자열로 업데이트하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenUpdateNameToEmpty() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act & Assert
            assertThatThrownBy(() -> product.update("", 2L, 1800000L, null, null, ProductStatus.SALE))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }
    }

    @Nested
    @DisplayName("Product status - isAvailable")
    class IsAvailable {

        @Test
        @DisplayName("SALE 상태이고 삭제되지 않으면 true를 반환한다")
        void returnsTrue_whenSaleAndNotDeleted() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act & Assert
            assertThat(product.isAvailable()).isTrue();
        }

        @Test
        @DisplayName("STOP 상태이면 false를 반환한다")
        void returnsFalse_whenStop() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            product.update("아이폰 15", 1L, 1500000L, null, null, ProductStatus.STOP);

            // Act & Assert
            assertThat(product.isAvailable()).isFalse();
        }

        @Test
        @DisplayName("삭제된 상태이면 false를 반환한다")
        void returnsFalse_whenDeleted() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            product.delete();

            // Act & Assert
            assertThat(product.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("Product delete")
    class Delete {

        @Test
        @DisplayName("delete 호출 시 deletedAt이 설정된다")
        void setsDeletedAt_whenDeleteCalled() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act
            product.delete();

            // Assert
            assertThat(product.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("이미 삭제된 상태에서 delete 호출해도 예외가 발생하지 않는다 (멱등성)")
        void doesNotThrow_whenDeleteCalledTwice() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            product.delete();

            // Act & Assert (멱등성)
            product.delete();
            assertThat(product.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("likeCount - 좋아요 수 관리")
    class LikeCount {

        @Test
        @DisplayName("생성 시 likeCount 기본값은 0이다")
        void likeCountIsZero_whenCreated() {
            // Arrange & Act
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Assert
            assertThat(product.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("increaseLikeCount 호출 시 likeCount가 1 증가한다")
        void increasesLikeCount() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act
            product.increaseLikeCount();

            // Assert
            assertThat(product.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("decreaseLikeCount 호출 시 likeCount가 1 감소한다")
        void decreasesLikeCount() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);
            product.increaseLikeCount();
            product.increaseLikeCount();

            // Act
            product.decreaseLikeCount();

            // Assert
            assertThat(product.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("likeCount가 0일 때 decreaseLikeCount 호출해도 0 미만이 되지 않는다")
        void doesNotGoBelowZero_whenDecreaseCalledAtZero() {
            // Arrange
            Product product = new Product("아이폰 15", 1L, 1L, 1500000L);

            // Act
            product.decreaseLikeCount();

            // Assert
            assertThat(product.getLikeCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("DB 조회 데이터 복원 (toDomain)")
    class RestoreFromDatabase {

        @Test
        @DisplayName("DB에서 조회한 데이터로 Product 도메인 객체를 복원한다")
        void restoresProductFromDatabaseRecord() {
            // Arrange
            LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
            LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 10, 0);

            // Act
            Product product = new Product(
                1L, "아이폰 15", "20240101-00001", 1L, 1L, 1500000L,
                ProductStatus.SALE, 100000L, DiscountType.PRICE, 0L,
                createdAt, updatedAt, null
            );

            // Assert
            assertAll(
                () -> assertThat(product.getId()).isEqualTo(1L),
                () -> assertThat(product.getName()).isEqualTo("아이폰 15"),
                () -> assertThat(product.getProductCode()).isEqualTo("20240101-00001"),
                () -> assertThat(product.getBrandId()).isEqualTo(1L),
                () -> assertThat(product.getCategoryId()).isEqualTo(1L),
                () -> assertThat(product.getBasePrice()).isEqualTo(1500000L),
                () -> assertThat(product.getStatus()).isEqualTo(ProductStatus.SALE),
                () -> assertThat(product.getDiscount()).isEqualTo(100000L),
                () -> assertThat(product.getDiscountType()).isEqualTo(DiscountType.PRICE),
                () -> assertThat(product.isDeleted()).isFalse()
            );
        }

        @Test
        @DisplayName("삭제된 상품 데이터를 복원하면 isDeleted가 true를 반환한다")
        void returnsTrue_whenRestoredProductWasDeleted() {
            // Arrange
            LocalDateTime now = LocalDateTime.now();

            // Act
            Product product = new Product(
                1L, "아이폰 15", "20240101-00001", 1L, 1L, 1500000L,
                ProductStatus.SALE, null, null, 0L,
                now, now, now
            );

            // Assert
            assertThat(product.isDeleted()).isTrue();
        }
    }
}