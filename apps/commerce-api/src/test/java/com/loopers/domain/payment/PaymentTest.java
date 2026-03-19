package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class PaymentTest {

    @DisplayName("Payment 생성")
    @Nested
    class Create {

        @Test
        @DisplayName("유효한 입력으로 생성하면 REQUESTED 상태로 생성된다")
        void createsPayment_withValidInputs() {
            // arrange
            Long orderId = 1L;
            Long memberId = 1L;
            String orderNumber = "ORD20250318-0000001";
            String cardType = "VISA";
            String cardNo = "4111111111111111";
            Long amount = 10000L;

            // act
            Payment result = new Payment(orderId, memberId, orderNumber, cardType, cardNo, amount);

            // assert
            assertAll(
                () -> assertThat(result.getOrderId()).isEqualTo(orderId),
                () -> assertThat(result.getMemberId()).isEqualTo(memberId),
                () -> assertThat(result.getOrderNumber()).isEqualTo(orderNumber),
                () -> assertThat(result.getCardType()).isEqualTo(cardType),
                () -> assertThat(result.getCardNo()).isEqualTo(cardNo),
                () -> assertThat(result.getAmount()).isEqualTo(amount),
                () -> assertThat(result.getStatus()).isEqualTo(PaymentStatus.REQUESTED),
                () -> assertThat(result.getTransactionId()).isNull(),
                () -> assertThat(result.getFailReason()).isNull(),
                () -> assertThat(result.getCallbackReceivedAt()).isNull()
            );
        }

        @Test
        @DisplayName("orderId가 null이면 예외가 발생한다")
        void throwsException_whenOrderIdIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Payment(null, 1L, "ORD20250318-0000001", "VISA", "4111111111111111", 10000L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("memberId가 null이면 예외가 발생한다")
        void throwsException_whenMemberIdIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Payment(1L, null, "ORD20250318-0000001", "VISA", "4111111111111111", 10000L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("amount가 0 이하이면 예외가 발생한다")
        void throwsException_whenAmountIsZeroOrNegative() {
            // act & assert
            assertAll(
                () -> assertThatThrownBy(() -> new Payment(1L, 1L, "ORD20250318-0000001", "VISA", "4111111111111111", 0L))
                    .isInstanceOf(CoreException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.BAD_REQUEST),
                () -> assertThatThrownBy(() -> new Payment(1L, 1L, "ORD20250318-0000001", "VISA", "4111111111111111", -1L))
                    .isInstanceOf(CoreException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.BAD_REQUEST)
            );
        }

        @Test
        @DisplayName("cardType이 null이면 예외가 발생한다")
        void throwsException_whenCardTypeIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Payment(1L, 1L, "ORD20250318-0000001", null, "4111111111111111", 10000L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("cardNo가 null이면 예외가 발생한다")
        void throwsException_whenCardNoIsNull() {
            // act & assert
            assertThatThrownBy(() -> new Payment(1L, 1L, "ORD20250318-0000001", "VISA", null, 10000L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("transactionId 할당")
    @Nested
    class AssignTransactionId {

        @Test
        @DisplayName("assignTransactionId()로 transactionId를 할당할 수 있다")
        void assignsTransactionId() {
            // arrange
            Payment payment = createPayment();
            String transactionId = "TXN-12345";

            // act
            payment.assignTransactionId(transactionId);

            // assert
            assertThat(payment.getTransactionId()).isEqualTo(transactionId);
        }

        @Test
        @DisplayName("이미 transactionId가 있으면 예외가 발생한다")
        void throwsException_whenTransactionIdAlreadyAssigned() {
            // arrange
            Payment payment = createPayment();
            payment.assignTransactionId("TXN-12345");

            // act & assert
            assertThatThrownBy(() -> payment.assignTransactionId("TXN-99999"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("상태 전이")
    @Nested
    class StatusTransition {

        @Test
        @DisplayName("REQUESTED에서 markSuccess() → SUCCESS")
        void markSuccess_fromRequested() {
            // arrange
            Payment payment = createPayment();

            // act
            payment.markSuccess();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("REQUESTED에서 markFailed() → FAILED (failReason 저장)")
        void markFailed_fromRequested() {
            // arrange
            Payment payment = createPayment();
            String reason = "잔액 부족";

            // act
            payment.markFailed(reason);

            // assert
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getFailReason()).isEqualTo(reason)
            );
        }

        @Test
        @DisplayName("REQUESTED에서 markTimeout() → TIMEOUT")
        void markTimeout_fromRequested() {
            // arrange
            Payment payment = createPayment();

            // act
            payment.markTimeout();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.TIMEOUT);
        }

        @Test
        @DisplayName("TIMEOUT에서 markSuccess() → SUCCESS (콜백 도착 케이스)")
        void markSuccess_fromTimeout() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.TIMEOUT);

            // act
            payment.markSuccess();

            // assert
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        @Test
        @DisplayName("TIMEOUT에서 markFailed() → FAILED")
        void markFailed_fromTimeout() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.TIMEOUT);
            String reason = "PG 처리 실패";

            // act
            payment.markFailed(reason);

            // assert
            assertAll(
                () -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED),
                () -> assertThat(payment.getFailReason()).isEqualTo(reason)
            );
        }

        @Test
        @DisplayName("SUCCESS에서 markFailed() → 예외 (이미 최종 상태)")
        void throwsException_whenMarkFailed_fromSuccess() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);

            // act & assert
            assertThatThrownBy(() -> payment.markFailed("실패 사유"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("FAILED에서 markSuccess() → 예외 (이미 최종 상태)")
        void throwsException_whenMarkSuccess_fromFailed() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.FAILED);

            // act & assert
            assertThatThrownBy(() -> payment.markSuccess())
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("최종 상태 확인")
    @Nested
    class IsTerminal {

        @Test
        @DisplayName("SUCCESS는 최종 상태이다")
        void success_isTerminal() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.SUCCESS);

            // act & assert
            assertThat(payment.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("FAILED는 최종 상태이다")
        void failed_isTerminal() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.FAILED);

            // act & assert
            assertThat(payment.isTerminal()).isTrue();
        }

        @Test
        @DisplayName("REQUESTED는 최종 상태가 아니다")
        void requested_isNotTerminal() {
            // arrange
            Payment payment = createPayment();

            // act & assert
            assertThat(payment.isTerminal()).isFalse();
        }

        @Test
        @DisplayName("TIMEOUT은 최종 상태가 아니다")
        void timeout_isNotTerminal() {
            // arrange
            Payment payment = createPaymentWithStatus(PaymentStatus.TIMEOUT);

            // act & assert
            assertThat(payment.isTerminal()).isFalse();
        }
    }

    private Payment createPayment() {
        return new Payment(1L, 1L, "ORD20250318-0000001", "VISA", "4111111111111111", 10000L);
    }

    private Payment createPaymentWithStatus(PaymentStatus status) {
        return new Payment(1L, 1L, 1L, "ORD20250318-0000001", null, "VISA", "4111111111111111",
            10000L, status, null, null);
    }
}
