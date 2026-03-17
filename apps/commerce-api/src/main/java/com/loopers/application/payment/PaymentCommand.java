package com.loopers.application.payment;

public class PaymentCommand {

    public record Create(
        Long orderId,
        String cardType,
        String cardNo
    ) {
    }
}
