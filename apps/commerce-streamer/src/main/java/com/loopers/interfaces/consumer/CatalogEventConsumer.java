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
public class CatalogEventConsumer {

    private final MetricsService metricsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {"catalog-events"},
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleCatalogEvents(
        List<ConsumerRecord<Object, Object>> messages,
        Acknowledgment acknowledgment
    ) {
        int failCount = 0;
        for (ConsumerRecord<Object, Object> message : messages) {
            try {
                JsonNode node = objectMapper.readTree(message.value().toString());
                String eventId = node.get("eventId").asText();
                String eventType = node.get("eventType").asText();
                String aggregateId = node.get("aggregateId").asText();

                metricsService.processEvent(eventId, eventType, aggregateId);
            } catch (Exception e) {
                failCount++;
                log.error("catalog-events 메시지 처리 실패: partition={}, offset={}, error={}",
                    message.partition(), message.offset(), e.getMessage());
            }
        }
        if (failCount > 0) {
            log.warn("catalog-events 배치 처리 완료: total={}, failed={}", messages.size(), failCount);
        }
        acknowledgment.acknowledge();
    }
}
