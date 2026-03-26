package com.loopers.application.kafka;

import com.loopers.application.event.OrderCompletedEvent;
import com.loopers.application.event.ProductLikedEvent;
import com.loopers.application.event.ProductUnlikedEvent;
import com.loopers.domain.outbox.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * ApplicationEvent를 수신하여 Outbox 테이블에 기록하는 리스너.
 *
 * @EventListener를 사용하여 비즈니스 TX 안에서 동기 실행됨.
 * → outbox INSERT가 비즈니스 데이터와 같은 TX에서 원자적으로 저장됨.
 *
 * Kafka 전파 대상 이벤트만 기록:
 * - ProductLikedEvent → catalog-events 토픽
 * - ProductUnlikedEvent → catalog-events 토픽
 * - OrderCompletedEvent → order-events 토픽
 */
@Component
@RequiredArgsConstructor
public class OutboxEventRecorder {

    private final OutboxEventService outboxEventService;

    @EventListener
    public void handleProductLiked(ProductLikedEvent event) {
        outboxEventService.recordEvent(
            "PRODUCT",
            String.valueOf(event.productId()),
            "PRODUCT_LIKED",
            "{\"productId\":" + event.productId() + "}"
        );
    }

    @EventListener
    public void handleProductUnliked(ProductUnlikedEvent event) {
        outboxEventService.recordEvent(
            "PRODUCT",
            String.valueOf(event.productId()),
            "PRODUCT_UNLIKED",
            "{\"productId\":" + event.productId() + "}"
        );
    }

    @EventListener
    public void handleOrderCompleted(OrderCompletedEvent event) {
        outboxEventService.recordEvent(
            "ORDER",
            String.valueOf(event.orderId()),
            "ORDER_COMPLETED",
            "{\"orderId\":" + event.orderId()
                + ",\"memberId\":" + event.memberId()
                + ",\"totalAmount\":" + event.totalAmount() + "}"
        );
    }
}
