package com.loopers.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        applicationEventPublisher.publishEvent(new UserActionEvent(
            event.memberId(),
            ActionType.PAYMENT,
            event.paymentId(),
            "PAYMENT",
            "{\"orderId\":" + event.orderId() + ",\"amount\":" + event.amount() + "}"
        ));
    }
}
