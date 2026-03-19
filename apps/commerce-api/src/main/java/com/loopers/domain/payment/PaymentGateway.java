package com.loopers.domain.payment;

public interface PaymentGateway {

    boolean isAvailable();

    PaymentGatewayResponse requestPayment(
        String orderNumber, String cardType, String cardNo,
        Long amount, Long memberId);

    PaymentGatewayResponse getPaymentStatus(String transactionId, Long memberId);

    PaymentGatewayResponse getPaymentByOrderId(String orderNumber, Long memberId);
}
