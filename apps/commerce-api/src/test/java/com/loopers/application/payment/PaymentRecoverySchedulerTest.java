package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentRecoverySchedulerTest {

    private PaymentRecoveryScheduler paymentRecoveryScheduler;

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentFacade paymentFacade;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        paymentRecoveryScheduler = new PaymentRecoveryScheduler(paymentService, paymentFacade, 100);
    }

    @Test
    @DisplayName("미완료 결제 2건 존재 → 각각 recoverPayment() 호출")
    void recoversEachPendingPayment() {
        // arrange
        Payment payment1 = new Payment(1L, 100L, 1L, "ORD-001",
            "TXN-001", "VISA", "4111111111111111",
            10000L, PaymentStatus.TIMEOUT, null, null);
        Payment payment2 = new Payment(2L, 101L, 1L, "ORD-002",
            null, "MASTER", "5111111111111111",
            20000L, PaymentStatus.REQUESTED, null, null);

        given(paymentService.getPaymentsForRecovery(
            eq(List.of(PaymentStatus.TIMEOUT, PaymentStatus.REQUESTED)),
            any(LocalDateTime.class),
            anyInt()
        )).willReturn(List.of(payment1, payment2));

        // act
        paymentRecoveryScheduler.recoverPendingPayments();

        // assert
        verify(paymentFacade).recoverPayment(payment1);
        verify(paymentFacade).recoverPayment(payment2);
        verify(paymentFacade, times(2)).recoverPayment(any(Payment.class));
    }

    @Test
    @DisplayName("미완료 결제 0건 → recoverPayment() 호출 안 됨")
    void doesNothing_whenNoPendingPayments() {
        // arrange
        given(paymentService.getPaymentsForRecovery(
            eq(List.of(PaymentStatus.TIMEOUT, PaymentStatus.REQUESTED)),
            any(LocalDateTime.class),
            anyInt()
        )).willReturn(List.of());

        // act
        paymentRecoveryScheduler.recoverPendingPayments();

        // assert
        verify(paymentFacade, never()).recoverPayment(any(Payment.class));
    }

    @Test
    @DisplayName("recoverPayment() 예외 발생 → 나머지 건은 계속 처리")
    void continuesProcessing_whenOneRecoveryFails() {
        // arrange
        Payment payment1 = new Payment(1L, 100L, 1L, "ORD-001",
            "TXN-001", "VISA", "4111111111111111",
            10000L, PaymentStatus.TIMEOUT, null, null);
        Payment payment2 = new Payment(2L, 101L, 1L, "ORD-002",
            "TXN-002", "MASTER", "5111111111111111",
            20000L, PaymentStatus.TIMEOUT, null, null);

        given(paymentService.getPaymentsForRecovery(
            eq(List.of(PaymentStatus.TIMEOUT, PaymentStatus.REQUESTED)),
            any(LocalDateTime.class),
            anyInt()
        )).willReturn(List.of(payment1, payment2));
        given(paymentFacade.recoverPayment(payment1)).willThrow(new RuntimeException("PG 연결 실패"));

        // act
        paymentRecoveryScheduler.recoverPendingPayments();

        // assert
        verify(paymentFacade).recoverPayment(payment1);
        verify(paymentFacade).recoverPayment(payment2);
    }
}
