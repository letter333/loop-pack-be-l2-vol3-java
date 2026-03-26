package com.loopers.application.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("OutboxPublisher 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("미발행 이벤트를 Kafka로 발행하고 published 처리한다")
    void publishesUnpublishedEvents() {
        // Arrange
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper, 100);
        OutboxEvent event = new OutboxEvent(1L, "PRODUCT", "100", "PRODUCT_LIKED",
            "{\"productId\":100}", ZonedDateTime.now(), null);

        given(outboxEventRepository.findUnpublished(100)).willReturn(List.of(event));
        given(kafkaTemplate.send(eq("catalog-events"), eq("100"), any()))
            .willReturn(CompletableFuture.completedFuture(null));

        // Act
        publisher.publishOutboxEvents();

        // Assert
        verify(kafkaTemplate).send(eq("catalog-events"), eq("100"), any());
        verify(outboxEventRepository).markPublished(1L);
    }

    @Test
    @DisplayName("ORDER 이벤트는 order-events 토픽으로 발행한다")
    void publishesOrderEventsToOrderTopic() {
        // Arrange
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper, 100);
        OutboxEvent event = new OutboxEvent(2L, "ORDER", "200", "ORDER_COMPLETED",
            "{\"orderId\":200}", ZonedDateTime.now(), null);

        given(outboxEventRepository.findUnpublished(100)).willReturn(List.of(event));
        given(kafkaTemplate.send(eq("order-events"), eq("200"), any()))
            .willReturn(CompletableFuture.completedFuture(null));

        // Act
        publisher.publishOutboxEvents();

        // Assert
        verify(kafkaTemplate).send(eq("order-events"), eq("200"), any());
        verify(outboxEventRepository).markPublished(2L);
    }

    @Test
    @DisplayName("미발행 이벤트가 없으면 아무것도 하지 않는다")
    void doesNothing_whenNoUnpublishedEvents() {
        // Arrange
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper, 100);
        given(outboxEventRepository.findUnpublished(100)).willReturn(List.of());

        // Act
        publisher.publishOutboxEvents();

        // Assert
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Kafka 발행 실패 시 markPublished를 호출하지 않는다")
    void doesNotMarkPublished_whenKafkaSendFails() {
        // Arrange
        OutboxPublisher publisher = new OutboxPublisher(outboxEventRepository, kafkaTemplate, objectMapper, 100);
        OutboxEvent event = new OutboxEvent(3L, "PRODUCT", "300", "PRODUCT_LIKED",
            "{\"productId\":300}", ZonedDateTime.now(), null);

        given(outboxEventRepository.findUnpublished(100)).willReturn(List.of(event));
        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka 연결 실패"));
        given(kafkaTemplate.send(any(), any(), any())).willReturn((CompletableFuture) failedFuture);

        // Act
        publisher.publishOutboxEvents();

        // Assert
        verify(outboxEventRepository, never()).markPublished(3L);
    }
}
