package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("ProductOption 도메인 단위 테스트")
class ProductOptionTest {

    @Nested
    @DisplayName("ProductOption 생성")
    class Create {

        @Test
        @DisplayName("모든 필수값이 유효하면 정상적으로 생성된다")
        void createsProductOption_whenAllFieldsAreValid() {
            // Arrange & Act
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Assert
            assertAll(
                () -> assertThat(option.getProductId()).isEqualTo(1L),
                () -> assertThat(option.getOptionValue()).isEqualTo("BLACK_M"),
                () -> assertThat(option.getDisplayName()).isEqualTo("블랙 / M"),
                () -> assertThat(option.getExtraPrice()).isEqualTo(5000L),
                () -> assertThat(option.getStockQuantity()).isEqualTo(100)
            );
        }

        @Test
        @DisplayName("productId가 null이면 정상 생성된다 (애그리거트 루트를 통해 설정)")
        void createsProductOption_whenProductIdIsNull() {
            // Arrange & Act
            ProductOption option = new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 100);

            // Assert
            assertThat(option.getProductId()).isNull();
        }

        @Test
        @DisplayName("optionValue가 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenOptionValueIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> new ProductOption(1L, null, "블랙 / M", 5000L, 100))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("optionValue가 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenOptionValueIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> new ProductOption(1L, "", "블랙 / M", 5000L, 100))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("stockQuantity가 음수이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStockQuantityIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, -1))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("extraPrice가 null이면 0으로 설정된다")
        void setsZero_whenExtraPriceIsNull() {
            // Arrange & Act
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", null, 100);

            // Assert
            assertThat(option.getExtraPrice()).isEqualTo(0L);
        }

        @Test
        @DisplayName("stockQuantity가 0이면 정상 생성된다")
        void createsProductOption_whenStockQuantityIsZero() {
            // Arrange & Act
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 0);

            // Assert
            assertThat(option.getStockQuantity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("재고 차감")
    class DecreaseStock {

        @Test
        @DisplayName("재고가 충분하면 정상적으로 차감된다")
        void decreasesStock_whenStockIsSufficient() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Act
            option.decreaseStock(30);

            // Assert
            assertThat(option.getStockQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStockIsInsufficient() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 10);

            // Act & Assert
            assertThatThrownBy(() -> option.decreaseStock(20))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("차감 수량이 0이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenQuantityIsZero() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Act & Assert
            assertThatThrownBy(() -> option.decreaseStock(0))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("차감 수량이 음수이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenQuantityIsNegative() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Act & Assert
            assertThatThrownBy(() -> option.decreaseStock(-5))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("재고 전량을 차감하면 재고가 0이 된다")
        void decreasesToZero_whenDecreasingAllStock() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 50);

            // Act
            option.decreaseStock(50);

            // Assert
            assertThat(option.getStockQuantity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("재고 증가")
    class IncreaseStock {

        @Test
        @DisplayName("재고가 정상적으로 증가한다")
        void increasesStock() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Act
            option.increaseStock(50);

            // Assert
            assertThat(option.getStockQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("증가 수량이 0이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenQuantityIsZero() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Act & Assert
            assertThatThrownBy(() -> option.increaseStock(0))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("증가 수량이 음수이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenQuantityIsNegative() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, 100);

            // Act & Assert
            assertThatThrownBy(() -> option.increaseStock(-10))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("재고 증가 시 상한선(Integer.MAX_VALUE)을 초과하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStockExceedsMaxValue() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, Integer.MAX_VALUE - 10);

            // Act & Assert
            assertThatThrownBy(() -> option.increaseStock(20))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("재고 증가 시 정확히 상한선(Integer.MAX_VALUE)이면 정상적으로 증가한다")
        void increasesStock_whenResultEqualsMaxValue() {
            // Arrange
            ProductOption option = new ProductOption(1L, "BLACK_M", "블랙 / M", 5000L, Integer.MAX_VALUE - 10);

            // Act
            option.increaseStock(10);

            // Assert
            assertThat(option.getStockQuantity()).isEqualTo(Integer.MAX_VALUE);
        }
    }
}