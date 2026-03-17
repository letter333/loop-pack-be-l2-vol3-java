package com.loopers.infrastructure.payment;

public class PgPaymentFailedException extends RuntimeException {

    public PgPaymentFailedException(String message) {
        super(message);
    }
}
