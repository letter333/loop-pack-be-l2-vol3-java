package com.loopers.infrastructure.payment;

public record PgPaymentRequest(
    String orderId,
    String cardType,
    String cardNo,
    String amount,
    String callbackUrl
) {
}
