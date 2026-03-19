package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private static final Long PAYMENT_ID = 1L;
    private static final Long ORDER_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final String TRANSACTION_ID = "TXN-12345";

    @DisplayName("결제 저장")
    @Nested
    class Save {

        @Test
        @DisplayName("Payment를 저장하고 반환한다")
        void savesAndReturnsPayment() {
            // arrange
            Payment payment = createPayment();
            given(paymentRepository.save(payment)).willReturn(payment);

            // act
            Payment result = paymentService.save(payment);

            // assert
            assertAll(
                () -> assertThat(result.getOrderId()).isEqualTo(ORDER_ID),
                () -> verify(paymentRepository).save(payment)
            );
        }
    }

    @DisplayName("결제 조회 (ID)")
    @Nested
    class GetPayment {

        @Test
        @DisplayName("존재하면 반환한다")
        void returnsPayment_whenExists() {
            // arrange
            Payment payment = createPaymentWithId(PAYMENT_ID);
            given(paymentRepository.findById(PAYMENT_ID)).willReturn(Optional.of(payment));

            // act
            Payment result = paymentService.getPayment(PAYMENT_ID);

            // assert
            assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않으면 NOT_FOUND 예외")
        void throwsException_whenNotFound() {
            // arrange
            given(paymentRepository.findById(999L)).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> paymentService.getPayment(999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("결제 조회 (transactionId)")
    @Nested
    class GetPaymentByTransactionId {

        @Test
        @DisplayName("존재하면 반환한다")
        void returnsPayment_whenExists() {
            // arrange
            Payment payment = createPaymentWithId(PAYMENT_ID);
            given(paymentRepository.findByTransactionId(TRANSACTION_ID)).willReturn(Optional.of(payment));

            // act
            Payment result = paymentService.getPaymentByTransactionId(TRANSACTION_ID);

            // assert
            assertThat(result.getId()).isEqualTo(PAYMENT_ID);
        }

        @Test
        @DisplayName("존재하지 않으면 NOT_FOUND 예외")
        void throwsException_whenNotFound() {
            // arrange
            given(paymentRepository.findByTransactionId("UNKNOWN")).willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> paymentService.getPaymentByTransactionId("UNKNOWN"))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("결제 조회 (orderId)")
    @Nested
    class GetPaymentsByOrderId {

        @Test
        @DisplayName("해당 주문의 결제 목록을 반환한다")
        void returnsPayments_byOrderId() {
            // arrange
            Payment payment1 = createPaymentWithId(1L);
            Payment payment2 = createPaymentWithId(2L);
            given(paymentRepository.findByOrderId(ORDER_ID)).willReturn(List.of(payment1, payment2));

            // act
            List<Payment> result = paymentService.getPaymentsByOrderId(ORDER_ID);

            // assert
            assertThat(result).hasSize(2);
        }
    }

    private Payment createPayment() {
        return new Payment(ORDER_ID, MEMBER_ID, "ORD20250318-0000001", "VISA", "4111111111111111", 10000L);
    }

    private Payment createPaymentWithId(Long id) {
        return new Payment(id, ORDER_ID, MEMBER_ID, "ORD20250318-0000001", TRANSACTION_ID,
            "VISA", "4111111111111111", 10000L, PaymentStatus.REQUESTED, null, null);
    }
}
