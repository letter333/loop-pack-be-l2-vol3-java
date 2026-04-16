package com.loopers.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.kafka.KafkaTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewedEventListener {

    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleProductViewed(ProductViewedEvent event) {
        try {
            String message = buildMessage(event);
            kafkaTemplate.send(KafkaTopic.CATALOG_EVENTS, String.valueOf(event.productId()), message);
        } catch (Exception e) {
            log.warn("PRODUCT_VIEWED Kafka 전송 실패 (유실 허용): productId={}, error={}",
                event.productId(), e.getMessage());
        }
    }

    private String buildMessage(ProductViewedEvent event) {
        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("eventId", UUID.randomUUID().toString());
            message.put("eventType", "PRODUCT_VIEWED");
            message.put("aggregateId", String.valueOf(event.productId()));
            message.put("payload", Map.of("productId", event.productId()));
            message.put("createdAt", String.valueOf(ZonedDateTime.now()));
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("PRODUCT_VIEWED 메시지 직렬화 실패", e);
        }
    }
}
