package com.loopers.infrastructure.payment;

public record PgPaymentStatusResponse(
    String transactionId,
    String orderId,
    String status,
    String message
) {
}
