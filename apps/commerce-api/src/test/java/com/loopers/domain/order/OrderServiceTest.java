package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 1L;

    @DisplayName("주문 조회")
    @Nested
    class GetOrder {

        @Test
        @DisplayName("주문이 존재하면 반환한다")
        void returnsOrder_whenExists() {
            // arrange
            Order order = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));

            // act
            Order result = orderService.getOrder(ORDER_ID);

            // assert
            assertThat(result.getId()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("주문이 존재하지 않으면 NOT_FOUND 예외가 발생한다")
        void throwsException_whenNotFound() {
            // arrange
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> orderService.getOrder(999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("주문 목록 조회")
    @Nested
    class GetOrders {

        @Test
        @DisplayName("memberId와 기간으로 주문 목록을 조회한다")
        void returnsOrders_byMemberIdAndPeriod() {
            // arrange
            Order order1 = createOrder(1L, MEMBER_ID, OrderStatus.PENDING);
            Order order2 = createOrder(2L, MEMBER_ID, OrderStatus.PAID);
            LocalDateTime startDate = LocalDateTime.now().minusMonths(3);
            given(orderRepository.findByMemberIdAndCreatedAtAfter(MEMBER_ID, startDate))
                .willReturn(List.of(order1, order2));

            // act
            List<Order> result = orderService.getOrders(MEMBER_ID, startDate);

            // assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("기간이 null이면 전체 기간을 조회한다")
        void returnsAllOrders_whenPeriodIsNull() {
            // arrange
            Order order = createOrder(1L, MEMBER_ID, OrderStatus.PENDING);
            given(orderRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(order));

            // act
            List<Order> result = orderService.getOrders(MEMBER_ID, null);

            // assert
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("주문이 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenNoOrders() {
            // arrange
            given(orderRepository.findByMemberId(MEMBER_ID)).willReturn(Collections.emptyList());

            // act
            List<Order> result = orderService.getOrders(MEMBER_ID, null);

            // assert
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @Test
        @DisplayName("주문을 저장하고 반환한다")
        void savesAndReturnsOrder() {
            // arrange
            Order order = new Order(MEMBER_ID, "홍길동", "010-1234-5678", null, "서울시", null, null);
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Order result = orderService.createOrder(order);

            // assert
            assertAll(
                () -> assertThat(result.getMemberId()).isEqualTo(MEMBER_ID),
                () -> verify(orderRepository).save(order)
            );
        }
    }

    @DisplayName("주문 취소")
    @Nested
    class CancelOrder {

        @Test
        @DisplayName("주문을 취소하고 반환한다")
        void cancelsAndReturnsOrder() {
            // arrange
            Order order = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            given(orderRepository.findById(ORDER_ID)).willReturn(Optional.of(order));
            given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            Order result = orderService.cancelOrder(ORDER_ID);

            // assert
            assertAll(
                () -> assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED),
                () -> verify(orderRepository).save(order)
            );
        }

        @Test
        @DisplayName("존재하지 않는 주문을 취소하면 NOT_FOUND 예외가 발생한다")
        void throwsException_whenOrderNotFound() {
            // arrange
            given(orderRepository.findById(999L)).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> orderService.cancelOrder(999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("소유권 검증")
    @Nested
    class ValidateOwnership {

        @Test
        @DisplayName("본인 주문이면 예외가 발생하지 않는다")
        void doesNotThrow_whenOwnerMatches() {
            // arrange
            Order order = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);

            // act & assert (no exception)
            orderService.validateOwnership(MEMBER_ID, order);
        }

        @Test
        @DisplayName("타인 주문이면 FORBIDDEN 예외가 발생한다")
        void throwsException_whenOwnerDoesNotMatch() {
            // arrange
            Order order = createOrder(ORDER_ID, 2L, OrderStatus.PENDING);

            // act & assert
            assertThatThrownBy(() -> orderService.validateOwnership(MEMBER_ID, order))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);
        }
    }

    private Order createOrder(Long id, Long memberId, OrderStatus status) {
        return new Order(
            id, memberId, "ORD20250225-0000001", "테스트 주문",
            "홍길동", "010-1234-5678", null, "서울시", null, null,
            status, 10000, 0, 0, 10000
        );
    }
}
