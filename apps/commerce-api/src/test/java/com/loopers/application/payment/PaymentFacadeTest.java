package com.loopers.application.payment;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentFacadeTest {

    @InjectMocks
    private PaymentFacade paymentFacade;

    @Mock
    private MemberService memberService;

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentGateway paymentGateway;

    private static final String LOGIN_ID = "user1";
    private static final String PASSWORD = "password1";
    private static final Long MEMBER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final String ORDER_NUMBER = "ORD20250318-0000001";
    private static final Long PAYMENT_AMOUNT = 50000L;

    private Member createMember() {
        return new Member(MEMBER_ID, LOGIN_ID, "encodedPw", "테스트유저", LocalDate.of(1990, 1, 1), "test@test.com");
    }

    private Order createOrder() {
        return new Order(ORDER_ID, MEMBER_ID, ORDER_NUMBER, "테스트 상품",
            "홍길동", "010-1234-5678", "12345", "서울시 강남구", "101호", "빠른 배송",
            OrderStatus.PENDING, 50000L, 0L, 0L, PAYMENT_AMOUNT);
    }

    private PaymentCommand.Create createCommand() {
        return new PaymentCommand.Create(ORDER_ID, "VISA", "4111111111111111");
    }

    @DisplayName("결제 요청")
    @Nested
    class RequestPayment {

        @Test
        @DisplayName("PG 요청 성공 시 REQUESTED 상태로 생성되고 transactionId가 할당된다")
        void createsPaymentAndAssignsTransactionId_whenPgSucceeds() {
            // arrange
            Member member = createMember();
            Order order = createOrder();
            PaymentCommand.Create command = createCommand();

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);
            given(paymentService.save(any(Payment.class))).willAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                return new Payment(1L, p.getOrderId(), p.getMemberId(), p.getOrderNumber(),
                    p.getTransactionId(), p.getCardType(), p.getCardNo(),
                    p.getAmount(), p.getStatus(), p.getFailReason(), p.getCallbackReceivedAt());
            });
            given(paymentGateway.requestPayment(
                ORDER_NUMBER, "VISA", "4111111111111111", PAYMENT_AMOUNT, MEMBER_ID
            )).willReturn(new PaymentGatewayResponse("TXN-001", "ACCEPTED", "결제 요청 접수"));

            // act
            PaymentInfo result = paymentFacade.requestPayment(LOGIN_ID, PASSWORD, command);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.REQUESTED),
                () -> assertThat(result.transactionId()).isEqualTo("TXN-001"),
                () -> assertThat(result.orderId()).isEqualTo(ORDER_ID),
                () -> assertThat(result.memberId()).isEqualTo(MEMBER_ID),
                () -> assertThat(result.amount()).isEqualTo(PAYMENT_AMOUNT),
                () -> assertThat(result.cardType()).isEqualTo("VISA"),
                () -> assertThat(result.cardNo()).isEqualTo("4111111111111111")
            );
        }

        @Test
        @DisplayName("PG 요청 실패(4xx) 시 FAILED 상태로 저장된다")
        void marksPaymentFailed_whenPgRejects() {
            // arrange
            Member member = createMember();
            Order order = createOrder();
            PaymentCommand.Create command = createCommand();

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);
            given(paymentService.save(any(Payment.class))).willAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                return new Payment(1L, p.getOrderId(), p.getMemberId(), p.getOrderNumber(),
                    p.getTransactionId(), p.getCardType(), p.getCardNo(),
                    p.getAmount(), p.getStatus(), p.getFailReason(), p.getCallbackReceivedAt());
            });
            given(paymentGateway.requestPayment(
                anyString(), anyString(), anyString(), anyLong(), anyLong()
            )).willThrow(new PaymentGatewayException("PG 결제 요청 실패", false));

            // act
            PaymentInfo result = paymentFacade.requestPayment(LOGIN_ID, PASSWORD, command);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(result.failReason()).isEqualTo("PG 결제 요청 실패")
            );
        }

        @Test
        @DisplayName("PG 타임아웃 시 TIMEOUT 상태로 저장된다")
        void marksPaymentTimeout_whenPgTimesOut() {
            // arrange
            Member member = createMember();
            Order order = createOrder();
            PaymentCommand.Create command = createCommand();

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);
            given(paymentService.save(any(Payment.class))).willAnswer(invocation -> {
                Payment p = invocation.getArgument(0);
                return new Payment(1L, p.getOrderId(), p.getMemberId(), p.getOrderNumber(),
                    p.getTransactionId(), p.getCardType(), p.getCardNo(),
                    p.getAmount(), p.getStatus(), p.getFailReason(), p.getCallbackReceivedAt());
            });
            given(paymentGateway.requestPayment(
                anyString(), anyString(), anyString(), anyLong(), anyLong()
            )).willThrow(new PaymentGatewayException("PG 결제 요청 타임아웃", true));

            // act
            PaymentInfo result = paymentFacade.requestPayment(LOGIN_ID, PASSWORD, command);

            // assert
            assertThat(result.status()).isEqualTo(PaymentStatus.TIMEOUT);
        }

        @Test
        @DisplayName("인증 실패 시 UNAUTHORIZED 예외가 발생한다")
        void throwsUnauthorized_whenAuthenticationFails() {
            // arrange
            PaymentCommand.Create command = createCommand();
            given(memberService.authenticate(LOGIN_ID, PASSWORD))
                .willThrow(new CoreException(ErrorType.UNAUTHORIZED));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED));
        }

        @Test
        @DisplayName("존재하지 않는 주문 시 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenOrderNotExists() {
            // arrange
            Member member = createMember();
            PaymentCommand.Create command = createCommand();

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID))
                .willThrow(new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("주문 소유자가 아닌 경우 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNotOrderOwner() {
            // arrange
            Member member = createMember();
            Order order = createOrder();
            PaymentCommand.Create command = createCommand();

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);
            willThrow(new CoreException(ErrorType.FORBIDDEN, "해당 주문에 대한 권한이 없습니다."))
                .given(orderService).validateOwnership(eq(MEMBER_ID), any(Order.class));

            // act & assert
            assertThatThrownBy(() -> paymentFacade.requestPayment(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }
}
