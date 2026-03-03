package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class OrderTest {

    @DisplayName("Order 생성")
    @Nested
    class Create {

        @Test
        @DisplayName("유효한 입력으로 Order를 생성하면 PENDING 상태로 생성된다")
        void createsOrder_withValidInputs() {
            // arrange
            Long memberId = 1L;
            String recipientName = "홍길동";
            String phone = "010-1234-5678";
            String zipCode = "06234";
            String address = "서울시 강남구";
            String addressDetail = "101호";
            String shippingMemo = "문 앞에 놔주세요";

            // act
            Order result = new Order(memberId, recipientName, phone, zipCode, address, addressDetail, shippingMemo);

            // assert
            assertAll(
                () -> assertThat(result.getMemberId()).isEqualTo(memberId),
                () -> assertThat(result.getRecipientName()).isEqualTo(recipientName),
                () -> assertThat(result.getPhone()).isEqualTo(phone),
                () -> assertThat(result.getZipCode()).isEqualTo(zipCode),
                () -> assertThat(result.getAddress()).isEqualTo(address),
                () -> assertThat(result.getAddressDetail()).isEqualTo(addressDetail),
                () -> assertThat(result.getShippingMemo()).isEqualTo(shippingMemo),
                () -> assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING)
            );
        }

        @Test
        @DisplayName("주문번호가 ORD{YYYYMMDD}-{7자리} 형식으로 자동 생성된다")
        void generatesOrderNumber_automaticallyWithCorrectFormat() {
            // arrange & act
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);

            // assert
            assertThat(order.getOrderNumber()).matches("ORD\\d{8}-\\d{7}");
        }

        @Test
        @DisplayName("memberId가 null이면 예외가 발생한다")
        void throwsException_whenMemberIdIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Order(null, "홍길동", "010-1234-5678", null, "서울시", null, null))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("상품 추가 및 금액 계산")
    @Nested
    class AddOrderProduct {

        @Test
        @DisplayName("addOrderProduct 시 orderProducts에 추가된다")
        void addsOrderProduct() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);
            OrderProduct orderProduct = new OrderProduct(1L, 10L, "상품1", "옵션1", 10000L, 0L, 1, null);

            // act
            order.addOrderProduct(orderProduct);

            // assert
            assertThat(order.getOrderProducts()).hasSize(1);
            assertThat(order.getOrderProducts().get(0)).isEqualTo(orderProduct);
        }

        @Test
        @DisplayName("상품이 1개일 때 orderName은 상품명으로 생성된다")
        void generatesOrderName_withSingleProduct() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);
            OrderProduct orderProduct = new OrderProduct(1L, 10L, "테스트상품", "옵션1", 10000L, 0L, 1, null);

            // act
            order.addOrderProduct(orderProduct);

            // assert
            assertThat(order.getOrderName()).isEqualTo("테스트상품");
        }

        @Test
        @DisplayName("상품이 2개 이상일 때 orderName은 '상품명 외 N건' 형식으로 생성된다")
        void generatesOrderName_withMultipleProducts() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);
            OrderProduct orderProduct1 = new OrderProduct(1L, 10L, "첫번째상품", "옵션1", 10000L, 0L, 1, null);
            OrderProduct orderProduct2 = new OrderProduct(2L, 20L, "두번째상품", "옵션2", 20000L, 0L, 1, null);

            // act
            order.addOrderProduct(orderProduct1);
            order.addOrderProduct(orderProduct2);

            // assert
            assertThat(order.getOrderName()).isEqualTo("첫번째상품 외 1건");
        }

        @Test
        @DisplayName("totalAmount가 orderProducts 합계로 계산된다")
        void calculatesTotalAmount() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);
            OrderProduct orderProduct1 = new OrderProduct(1L, 10L, "상품1", "옵션1", 10000L, 1000L, 2, null);
            OrderProduct orderProduct2 = new OrderProduct(2L, 20L, "상품2", "옵션2", 5000L, 0L, 3, null);

            // act
            order.addOrderProduct(orderProduct1);
            order.addOrderProduct(orderProduct2);

            // assert
            // (10000 + 1000) * 2 + 5000 * 3 = 22000 + 15000 = 37000
            assertThat(order.getTotalAmount()).isEqualTo(37000);
        }

        @Test
        @DisplayName("paymentAmount = totalAmount + shippingFee - discountAmount")
        void calculatesPaymentAmount() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);
            OrderProduct orderProduct = new OrderProduct(1L, 10L, "상품1", "옵션1", 10000L, 0L, 1, null);
            order.addOrderProduct(orderProduct);
            order.setShippingFee(3000L);
            order.setDiscountAmount(1000L);

            // act
            order.calculateAmounts();

            // assert
            // 10000 + 3000 - 1000 = 12000
            assertThat(order.getPaymentAmount()).isEqualTo(12000L);
        }
    }

    @DisplayName("취소 가능 여부")
    @Nested
    class CanCancel {

        @Test
        @DisplayName("PENDING 상태에서 canCancel()은 true")
        void returnsTrue_whenPending() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);

            // act & assert
            assertThat(order.canCancel()).isTrue();
        }

        @Test
        @DisplayName("PAID 상태에서 canCancel()은 true")
        void returnsTrue_whenPaid() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PAID);

            // act & assert
            assertThat(order.canCancel()).isTrue();
        }

        @Test
        @DisplayName("PREPARING 상태에서 canCancel()은 false")
        void returnsFalse_whenPreparing() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PREPARING);

            // act & assert
            assertThat(order.canCancel()).isFalse();
        }

        @Test
        @DisplayName("SHIPPING 상태에서 canCancel()은 false")
        void returnsFalse_whenShipping() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.SHIPPING);

            // act & assert
            assertThat(order.canCancel()).isFalse();
        }

        @Test
        @DisplayName("DELIVERED 상태에서 canCancel()은 false")
        void returnsFalse_whenDelivered() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.DELIVERED);

            // act & assert
            assertThat(order.canCancel()).isFalse();
        }
    }

    @DisplayName("취소")
    @Nested
    class Cancel {

        @Test
        @DisplayName("PENDING 상태에서 cancel() 시 CANCELLED로 변경된다")
        void cancels_whenPending() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PAID 상태에서 cancel() 시 CANCELLED로 변경된다")
        void cancels_whenPaid() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PAID);

            // act
            order.cancel();

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("PREPARING 상태에서 cancel() 시 예외가 발생한다")
        void throwsException_whenPreparing() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PREPARING);

            // act & assert
            assertThatThrownBy(() -> order.cancel())
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("cancel() 시 모든 OrderProduct도 CANCELLED로 변경된다")
        void cancelsAllOrderProducts() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);
            OrderProduct orderProduct1 = new OrderProduct(1L, 10L, "상품1", "옵션1", 10000L, 0L, 1, null);
            OrderProduct orderProduct2 = new OrderProduct(2L, 20L, "상품2", "옵션2", 20000L, 0L, 1, null);
            order.addOrderProduct(orderProduct1);
            order.addOrderProduct(orderProduct2);

            // act
            order.cancel();

            // assert
            assertAll(
                () -> assertThat(orderProduct1.getStatus()).isEqualTo(OrderProductStatus.CANCELLED),
                () -> assertThat(orderProduct2.getStatus()).isEqualTo(OrderProductStatus.CANCELLED)
            );
        }
    }

    @DisplayName("소유권 검증")
    @Nested
    class Ownership {

        @Test
        @DisplayName("본인 주문이면 isOwnedBy() true")
        void returnsTrue_whenOwnerMatches() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);

            // act & assert
            assertThat(order.isOwnedBy(1L)).isTrue();
        }

        @Test
        @DisplayName("타인 주문이면 isOwnedBy() false")
        void returnsFalse_whenOwnerDoesNotMatch() {
            // arrange
            Order order = new Order(1L, "홍길동", "010-1234-5678", null, "서울시", null, null);

            // act & assert
            assertThat(order.isOwnedBy(2L)).isFalse();
        }
    }

    @DisplayName("상태 전환 가능 여부 (canTransitionTo)")
    @Nested
    class CanTransitionTo {

        @Test
        @DisplayName("PENDING에서 PAID로 전환 가능")
        void canTransition_fromPending_toPaid() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PENDING);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.PAID)).isTrue();
        }

        @Test
        @DisplayName("PENDING에서 CANCELLED로 전환 가능")
        void canTransition_fromPending_toCancelled() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PENDING);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("PENDING에서 PREPARING으로 전환 불가")
        void cannotTransition_fromPending_toPreparing() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PENDING);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.PREPARING)).isFalse();
        }

        @Test
        @DisplayName("PAID에서 PREPARING으로 전환 가능")
        void canTransition_fromPaid_toPreparing() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PAID);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.PREPARING)).isTrue();
        }

        @Test
        @DisplayName("PAID에서 CANCELLED로 전환 가능")
        void canTransition_fromPaid_toCancelled() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PAID);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("PAID에서 SHIPPING으로 직접 전환 불가")
        void cannotTransition_fromPaid_toShipping() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PAID);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.SHIPPING)).isFalse();
        }

        @Test
        @DisplayName("PREPARING에서 SHIPPING으로 전환 가능")
        void canTransition_fromPreparing_toShipping() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PREPARING);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.SHIPPING)).isTrue();
        }

        @Test
        @DisplayName("PREPARING에서 CANCELLED로 전환 불가")
        void cannotTransition_fromPreparing_toCancelled() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PREPARING);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        }

        @Test
        @DisplayName("SHIPPING에서 DELIVERED로 전환 가능")
        void canTransition_fromShipping_toDelivered() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.SHIPPING);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
        }

        @Test
        @DisplayName("DELIVERED에서 RETURNED로 전환 가능")
        void canTransition_fromDelivered_toReturned() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.DELIVERED);

            // act & assert
            assertThat(order.canTransitionTo(OrderStatus.RETURNED)).isTrue();
        }

        @Test
        @DisplayName("CANCELLED에서는 어떤 상태로도 전환 불가")
        void cannotTransition_fromCancelled() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.CANCELLED);

            // act & assert
            assertAll(
                () -> assertThat(order.canTransitionTo(OrderStatus.PENDING)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.PAID)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.PREPARING)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.SHIPPING)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.DELIVERED)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.RETURNED)).isFalse()
            );
        }

        @Test
        @DisplayName("RETURNED에서는 어떤 상태로도 전환 불가")
        void cannotTransition_fromReturned() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.RETURNED);

            // act & assert
            assertAll(
                () -> assertThat(order.canTransitionTo(OrderStatus.PENDING)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.PAID)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.PREPARING)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.SHIPPING)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.DELIVERED)).isFalse(),
                () -> assertThat(order.canTransitionTo(OrderStatus.CANCELLED)).isFalse()
            );
        }
    }

    @DisplayName("상태 전환 (transitionTo)")
    @Nested
    class TransitionTo {

        @Test
        @DisplayName("유효한 상태 전환 시 상태가 변경된다")
        void changesStatus_whenValidTransition() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PAID);

            // act
            order.transitionTo(OrderStatus.PREPARING);

            // assert
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
        }

        @Test
        @DisplayName("유효하지 않은 상태 전환 시 예외가 발생한다")
        void throwsException_whenInvalidTransition() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PENDING);

            // act & assert
            assertThatThrownBy(() -> order.transitionTo(OrderStatus.SHIPPING))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("CANCELLED로 전환 시 모든 OrderProduct도 취소된다")
        void cancelsAllOrderProducts_whenTransitionToCancelled() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.PENDING);
            OrderProduct orderProduct1 = new OrderProduct(1L, 10L, "상품1", "옵션1", 10000L, 0L, 1, null);
            OrderProduct orderProduct2 = new OrderProduct(2L, 20L, "상품2", "옵션2", 20000L, 0L, 1, null);
            order.setOrderProducts(List.of(orderProduct1, orderProduct2));

            // act
            order.transitionTo(OrderStatus.CANCELLED);

            // assert
            assertAll(
                () -> assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> assertThat(orderProduct1.getStatus()).isEqualTo(OrderProductStatus.CANCELLED),
                () -> assertThat(orderProduct2.getStatus()).isEqualTo(OrderProductStatus.CANCELLED)
            );
        }

        @Test
        @DisplayName("CANCELLED 상태에서 전환 시도하면 예외가 발생한다")
        void throwsException_whenTransitionFromCancelled() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.CANCELLED);

            // act & assert
            assertThatThrownBy(() -> order.transitionTo(OrderStatus.PAID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("RETURNED 상태에서 전환 시도하면 예외가 발생한다")
        void throwsException_whenTransitionFromReturned() {
            // arrange
            Order order = createOrderWithStatus(OrderStatus.RETURNED);

            // act & assert
            assertThatThrownBy(() -> order.transitionTo(OrderStatus.CANCELLED))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    private Order createOrderWithStatus(OrderStatus status) {
        return new Order(
            null, 1L, "ORD20250225-0000001", "테스트 주문",
            "홍길동", "010-1234-5678", null, "서울시", null, null,
            status, 10000L, 0L, 0L, 10000L
        );
    }
}
