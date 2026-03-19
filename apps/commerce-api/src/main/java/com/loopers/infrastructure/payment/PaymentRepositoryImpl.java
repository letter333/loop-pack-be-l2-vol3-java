package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository paymentJpaRepository;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity;
        if (payment.getId() != null) {
            entity = paymentJpaRepository.findById(payment.getId())
                .orElseGet(() -> PaymentEntity.from(payment));
            entity.update(
                payment.getStatus(),
                payment.getTransactionId(),
                payment.getFailReason(),
                payment.getCallbackReceivedAt()
            );
        } else {
            entity = PaymentEntity.from(payment);
        }
        return paymentJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return paymentJpaRepository.findById(id)
            .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByTransactionId(String transactionId) {
        return paymentJpaRepository.findByTransactionId(transactionId)
            .map(PaymentEntity::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(Long orderId) {
        return paymentJpaRepository.findByOrderId(orderId)
            .stream()
            .map(PaymentEntity::toDomain)
            .toList();
    }

    @Override
    public List<Payment> findByStatusInAndCreatedBefore(List<PaymentStatus> statuses, LocalDateTime threshold, int limit) {
        return paymentJpaRepository.findByStatusInAndCreatedAtBefore(statuses, threshold, PageRequest.of(0, limit))
            .stream()
            .map(PaymentEntity::toDomain)
            .toList();
    }
}
