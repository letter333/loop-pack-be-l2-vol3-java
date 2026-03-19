package com.loopers.infrastructure.payment;

public record PgPaymentResponse(
    String transactionId,
    String status,
    String message
) {
}
