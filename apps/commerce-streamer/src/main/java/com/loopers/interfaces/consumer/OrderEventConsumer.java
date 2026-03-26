package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.MetricsService;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventConsumer {

    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {"order-events"},
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleOrderEvents(
        List<ConsumerRecord<Object, Object>> messages,
        Acknowledgment acknowledgment
    ) {
        for (ConsumerRecord<Object, Object> message : messages) {
            try {
                JsonNode node = objectMapper.readTree(message.value().toString());
                String eventId = node.get("eventId").asText();
                String eventType = node.get("eventType").asText();
                String aggregateId = node.get("aggregateId").asText();

                metricsService.processEvent(eventId, eventType, aggregateId);
            } catch (Exception e) {
                log.error("order-events 메시지 처리 실패: offset={}, error={}",
                    message.offset(), e.getMessage());
            }
        }
        acknowledgment.acknowledge();
    }
}
