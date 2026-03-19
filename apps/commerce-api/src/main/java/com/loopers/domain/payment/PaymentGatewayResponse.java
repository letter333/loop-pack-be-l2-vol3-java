package com.loopers.domain.payment;

public record PaymentGatewayResponse(
    String transactionId,
    String status,
    String message
) {
    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }

    public boolean isFailed() {
        return "FAILED".equals(status);
    }
}
