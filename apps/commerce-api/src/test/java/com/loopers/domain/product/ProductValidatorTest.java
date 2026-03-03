package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ProductValidator 단위 테스트")
class ProductValidatorTest {

    @Nested
    @DisplayName("validateName")
    class ValidateName {

        @Test
        @DisplayName("유효한 상품명이면 예외가 발생하지 않는다")
        void doesNotThrow_whenNameIsValid() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateName("아이폰 15"))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("상품명이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateName(null))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("상품명이 빈 문자열이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsEmpty() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateName(""))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("상품명이 공백만 있으면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenNameIsBlank() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateName("   "))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }
    }

    @Nested
    @DisplayName("validateBasePrice")
    class ValidateBasePrice {

        @Test
        @DisplayName("유효한 가격이면 예외가 발생하지 않는다")
        void doesNotThrow_whenBasePriceIsValid() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateBasePrice(1000L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("가격이 0이면 예외가 발생하지 않는다 (무료 상품 허용)")
        void doesNotThrow_whenBasePriceIsZero() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateBasePrice(0L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("가격이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenBasePriceIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateBasePrice(null))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("가격이 음수이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenBasePriceIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateBasePrice(-1L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }
    }

    @Nested
    @DisplayName("validateDiscount")
    class ValidateDiscount {

        @Test
        @DisplayName("할인 정보가 둘 다 null이면 예외가 발생하지 않는다")
        void doesNotThrow_whenBothAreNull() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateDiscount(null, null, 10000L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정액 할인이 유효하면 예외가 발생하지 않는다")
        void doesNotThrow_whenPriceDiscountIsValid() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateDiscount(1000L, DiscountType.PRICE, 10000L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정률 할인이 유효하면 예외가 발생하지 않는다")
        void doesNotThrow_whenRateDiscountIsValid() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateDiscount(50L, DiscountType.RATE, 10000L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("할인 금액이 음수이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenDiscountIsNegative() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateDiscount(-100L, DiscountType.PRICE, 10000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("할인 금액만 있고 타입이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenOnlyDiscountIsProvided() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateDiscount(1000L, null, 10000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("할인 타입만 있고 금액이 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenOnlyDiscountTypeIsProvided() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateDiscount(null, DiscountType.PRICE, 10000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("정률 할인이 100%를 초과하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenRateDiscountExceeds100() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateDiscount(101L, DiscountType.RATE, 10000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("정률 할인이 정확히 100%이면 예외가 발생하지 않는다")
        void doesNotThrow_whenRateDiscountIs100() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateDiscount(100L, DiscountType.RATE, 10000L))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정액 할인이 기본 가격을 초과하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenPriceDiscountExceedsBasePrice() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateDiscount(15000L, DiscountType.PRICE, 10000L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }

        @Test
        @DisplayName("정액 할인이 기본 가격과 같으면 예외가 발생하지 않는다")
        void doesNotThrow_whenPriceDiscountEqualsBasePrice() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateDiscount(10000L, DiscountType.PRICE, 10000L))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateStatus")
    class ValidateStatus {

        @Test
        @DisplayName("유효한 상태이면 예외가 발생하지 않는다")
        void doesNotThrow_whenStatusIsValid() {
            // Act & Assert
            assertThatCode(() -> ProductValidator.validateStatus(ProductStatus.SALE))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("상태가 null이면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStatusIsNull() {
            // Act & Assert
            assertThatThrownBy(() -> ProductValidator.validateStatus(null))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> {
                    CoreException coreEx = (CoreException) ex;
                    assert coreEx.getErrorType() == ErrorType.BAD_REQUEST;
                });
        }
    }
}
