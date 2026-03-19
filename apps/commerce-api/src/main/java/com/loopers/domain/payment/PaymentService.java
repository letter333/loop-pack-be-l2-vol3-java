package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Payment getPayment(Long paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Payment getPaymentByTransactionId(String transactionId) {
        return paymentRepository.findByTransactionId(transactionId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Payment> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Payment> getPaymentsForRecovery(List<PaymentStatus> statuses, LocalDateTime threshold, int limit) {
        return paymentRepository.findByStatusInAndCreatedBefore(statuses, threshold, limit);
    }
}
