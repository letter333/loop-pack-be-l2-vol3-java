package com.loopers.domain.payment;

public interface PaymentGateway {

    PaymentGatewayResponse requestPayment(
        String orderNumber, String cardType, String cardNo,
        Long amount, Long memberId);
}
