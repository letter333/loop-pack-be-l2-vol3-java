package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    List<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findByStatusInAndCreatedAtBefore(List<PaymentStatus> statuses, LocalDateTime threshold);
}
