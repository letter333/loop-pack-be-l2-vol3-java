package com.loopers.domain.outbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("OutboxEventService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @InjectMocks
    private OutboxEventService outboxEventService;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    @DisplayName("이벤트를 Outbox에 기록한다")
    void recordsEvent() {
        // Arrange
        String aggregateType = "PRODUCT";
        String aggregateId = "100";
        String eventType = "PRODUCT_LIKED";
        String payload = "{\"productId\":100}";

        given(outboxEventRepository.save(any(OutboxEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // Act
        OutboxEvent result = outboxEventService.recordEvent(aggregateType, aggregateId, eventType, payload);

        // Assert
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("PRODUCT");
        assertThat(saved.getAggregateId()).isEqualTo("100");
        assertThat(saved.getEventType()).isEqualTo("PRODUCT_LIKED");
        assertThat(saved.getPayload()).isEqualTo("{\"productId\":100}");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getPublishedAt()).isNull();
    }

    @Test
    @DisplayName("주문 완료 이벤트를 Outbox에 기록한다")
    void recordsOrderCompletedEvent() {
        // Arrange
        String payload = "{\"orderId\":200,\"memberId\":10,\"totalAmount\":50000}";

        given(outboxEventRepository.save(any(OutboxEvent.class)))
            .willAnswer(invocation -> invocation.getArgument(0));

        // Act
        outboxEventService.recordEvent("ORDER", "200", "ORDER_COMPLETED", payload);

        // Assert
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent saved = captor.getValue();
        assertThat(saved.getAggregateType()).isEqualTo("ORDER");
        assertThat(saved.getAggregateId()).isEqualTo("200");
        assertThat(saved.getEventType()).isEqualTo("ORDER_COMPLETED");
    }
}
