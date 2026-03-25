package com.loopers.application.kafka;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Outbox 테이블에서 미발행 이벤트를 폴링하여 Kafka로 발행하는 Scheduler.
 *
 * 동작 흐름:
 * 1. outbox_events에서 published_at IS NULL인 레코드를 조회 (ORDER BY created_at ASC)
 * 2. 각 이벤트를 KafkaTemplate으로 해당 토픽에 발행 (aggregateId를 Partition Key로 사용)
 * 3. 발행 성공 시 published_at을 업데이트하여 완료 처리
 * 4. 발행 실패 시 skip → 다음 폴링에서 재시도 (At Least Once)
 */
@Slf4j
@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final int batchSize;

    public OutboxPublisher(
        OutboxEventRepository outboxEventRepository,
        KafkaTemplate<Object, Object> kafkaTemplate,
        @Value("${outbox.publisher.batch-size:100}") int batchSize
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.interval-ms:5000}")
    public void publishOutboxEvents() {
        List<OutboxEvent> events = outboxEventRepository.findUnpublished(batchSize);

        for (OutboxEvent event : events) {
            try {
                String topic = resolveTopicName(event.getAggregateType());
                kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload()).get();
                outboxEventRepository.markPublished(event.getId());
            } catch (Exception e) {
                log.warn("Outbox 이벤트 발행 실패: eventId={}, error={}", event.getId(), e.getMessage());
            }
        }
    }

    private String resolveTopicName(String aggregateType) {
        return switch (aggregateType) {
            case "PRODUCT" -> KafkaTopic.CATALOG_EVENTS;
            case "ORDER" -> KafkaTopic.ORDER_EVENTS;
            case "COUPON" -> KafkaTopic.COUPON_ISSUE_REQUESTS;
            default -> throw new IllegalArgumentException("알 수 없는 aggregate type: " + aggregateType);
        };
    }
}
