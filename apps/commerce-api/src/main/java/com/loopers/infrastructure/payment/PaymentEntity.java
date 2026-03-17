package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payments_order_id", columnList = "order_id"),
        @Index(name = "idx_payments_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_payments_member_id", columnList = "member_id"),
        @Index(name = "idx_payments_status", columnList = "status")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "order_number", nullable = false, length = 20)
    private String orderNumber;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "card_type", nullable = false, length = 20)
    private String cardType;

    @Column(name = "card_no", nullable = false, length = 20)
    private String cardNo;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    @Column(name = "callback_received_at")
    private LocalDateTime callbackReceivedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity();
        entity.orderId = payment.getOrderId();
        entity.memberId = payment.getMemberId();
        entity.orderNumber = payment.getOrderNumber();
        entity.transactionId = payment.getTransactionId();
        entity.cardType = payment.getCardType();
        entity.cardNo = payment.getCardNo();
        entity.amount = payment.getAmount();
        entity.status = payment.getStatus();
        entity.failReason = payment.getFailReason();
        entity.callbackReceivedAt = payment.getCallbackReceivedAt();
        return entity;
    }

    public Payment toDomain() {
        return new Payment(
            id, orderId, memberId, orderNumber, transactionId,
            cardType, cardNo, amount, status, failReason, callbackReceivedAt
        );
    }

    public void update(PaymentStatus status, String transactionId,
                       String failReason, LocalDateTime callbackReceivedAt) {
        this.status = status;
        this.transactionId = transactionId;
        this.failReason = failReason;
        this.callbackReceivedAt = callbackReceivedAt;
    }
}
