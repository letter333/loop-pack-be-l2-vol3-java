package com.loopers.application.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;

public record PaymentInfo(
    Long id,
    Long orderId,
    Long memberId,
    String orderNumber,
    String transactionId,
    String cardType,
    String cardNo,
    Long amount,
    PaymentStatus status,
    String failReason
) {

    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
            payment.getId(),
            payment.getOrderId(),
            payment.getMemberId(),
            payment.getOrderNumber(),
            payment.getTransactionId(),
            payment.getCardType(),
            payment.getCardNo(),
            payment.getAmount(),
            payment.getStatus(),
            payment.getFailReason()
        );
    }
}
