package com.loopers.domain.payment;

public record PaymentGatewayResponse(
    String transactionId,
    String status,
    String message
) {
}
