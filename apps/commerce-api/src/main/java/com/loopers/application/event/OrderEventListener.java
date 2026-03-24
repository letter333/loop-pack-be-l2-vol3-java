package com.loopers.application.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        applicationEventPublisher.publishEvent(new UserActionEvent(
            event.memberId(),
            ActionType.ORDER,
            event.orderId(),
            "ORDER",
            "{\"totalAmount\":" + event.totalAmount() + "}"
        ));
    }
}
