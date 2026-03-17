package com.loopers.domain.payment;

import lombok.Getter;

@Getter
public class PaymentGatewayException extends RuntimeException {

    private final boolean timeout;

    public PaymentGatewayException(String message, boolean timeout) {
        super(message);
        this.timeout = timeout;
    }

    public PaymentGatewayException(String message, boolean timeout, Throwable cause) {
        super(message, cause);
        this.timeout = timeout;
    }
}
