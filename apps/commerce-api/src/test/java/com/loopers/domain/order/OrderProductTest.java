package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderProductTest {

    @DisplayName("OrderProduct 생성")
    @Nested
    class Create {

        @Test
        @DisplayName("유효한 입력으로 OrderProduct를 생성하면 NORMAL 상태로 생성된다")
        void createsOrderProduct_withValidInputs() {
            // arrange
            Long productId = 1L;
            Long productOptionId = 10L;
            String productName = "테스트 상품";
            String optionName = "옵션1";
            int price = 10000;
            int extraPrice = 1000;
            int quantity = 2;
            String thumbnailUrl = "https://example.com/image.jpg";

            // act
            OrderProduct result = new OrderProduct(
                productId, productOptionId, productName, optionName,
                price, extraPrice, quantity, thumbnailUrl
            );

            // assert
            assertAll(
                () -> assertThat(result.getProductId()).isEqualTo(productId),
                () -> assertThat(result.getProductOptionId()).isEqualTo(productOptionId),
                () -> assertThat(result.getProductName()).isEqualTo(productName),
                () -> assertThat(result.getOptionName()).isEqualTo(optionName),
                () -> assertThat(result.getPrice()).isEqualTo(price),
                () -> assertThat(result.getExtraPrice()).isEqualTo(extraPrice),
                () -> assertThat(result.getQuantity()).isEqualTo(quantity),
                () -> assertThat(result.getThumbnailUrl()).isEqualTo(thumbnailUrl),
                () -> assertThat(result.getStatus()).isEqualTo(OrderProductStatus.NORMAL)
            );
        }

        @Test
        @DisplayName("quantity가 0이면 예외가 발생한다")
        void throwsException_whenQuantityIsZero() {
            // act & assert
            assertThatThrownBy(() -> new OrderProduct(
                1L, 10L, "상품", "옵션", 10000, 0, 0, null
            ))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("quantity가 음수이면 예외가 발생한다")
        void throwsException_whenQuantityIsNegative() {
            // act & assert
            assertThatThrownBy(() -> new OrderProduct(
                1L, 10L, "상품", "옵션", 10000, 0, -1, null
            ))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("금액 계산")
    @Nested
    class CalculateTotalPrice {

        @Test
        @DisplayName("총 금액은 (price + extraPrice) * quantity로 계산된다")
        void calculatesTotalPrice() {
            // arrange
            OrderProduct orderProduct = new OrderProduct(
                1L, 10L, "상품", "옵션", 10000, 1000, 3, null
            );

            // act
            int result = orderProduct.calculateTotalPrice();

            // assert
            assertThat(result).isEqualTo((10000 + 1000) * 3);
        }

        @Test
        @DisplayName("extraPrice가 0인 경우에도 정상 계산된다")
        void calculatesTotalPrice_whenExtraPriceIsZero() {
            // arrange
            OrderProduct orderProduct = new OrderProduct(
                1L, 10L, "상품", "옵션", 10000, 0, 2, null
            );

            // act
            int result = orderProduct.calculateTotalPrice();

            // assert
            assertThat(result).isEqualTo(10000 * 2);
        }
    }

    @DisplayName("취소")
    @Nested
    class Cancel {

        @Test
        @DisplayName("NORMAL 상태에서 취소하면 CANCELLED로 변경된다")
        void cancels_whenStatusIsNormal() {
            // arrange
            OrderProduct orderProduct = new OrderProduct(
                1L, 10L, "상품", "옵션", 10000, 0, 1, null
            );

            // act
            orderProduct.cancel();

            // assert
            assertThat(orderProduct.getStatus()).isEqualTo(OrderProductStatus.CANCELLED);
        }

        @Test
        @DisplayName("이미 취소된 상태에서 재취소하면 예외가 발생한다")
        void throwsException_whenAlreadyCancelled() {
            // arrange
            OrderProduct orderProduct = new OrderProduct(
                1L, 10L, "상품", "옵션", 10000, 0, 1, null
            );
            orderProduct.cancel();

            // act & assert
            assertThatThrownBy(() -> orderProduct.cancel())
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
