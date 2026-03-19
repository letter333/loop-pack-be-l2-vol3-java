package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.payment.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class PaymentRecoveryScheduler {

    private final PaymentService paymentService;
    private final PaymentFacade paymentFacade;
    private final int batchSize;

    public PaymentRecoveryScheduler(
        PaymentService paymentService,
        PaymentFacade paymentFacade,
        @Value("${payment.recovery.batch-size:100}") int batchSize
    ) {
        this.paymentService = paymentService;
        this.paymentFacade = paymentFacade;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payment.recovery.interval-ms:60000}")
    public void recoverPendingPayments() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(1);
        List<Payment> payments = paymentService.getPaymentsForRecovery(
            List.of(PaymentStatus.TIMEOUT, PaymentStatus.REQUESTED), threshold, batchSize);

        for (Payment payment : payments) {
            try {
                paymentFacade.recoverPayment(payment);
            } catch (Exception e) {
                log.warn("결제 복구 실패: paymentId={}, error={}", payment.getId(), e.getMessage());
            }
        }
    }
}
