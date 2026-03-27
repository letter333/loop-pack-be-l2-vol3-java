package com.loopers.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        applicationEventPublisher.publishEvent(new UserActionEvent(
            event.memberId(),
            ActionType.PAYMENT,
            event.paymentId(),
            "PAYMENT",
            toJson(Map.of("orderId", event.orderId(), "amount", event.amount()))
        ));
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 직렬화 실패", e);
        }
    }
}
