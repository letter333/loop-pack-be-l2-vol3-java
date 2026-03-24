package com.loopers.application.event;

public record PaymentSuccessEvent(
    Long paymentId,
    Long orderId,
    Long memberId,
    Long amount
) {
}
