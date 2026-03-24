package com.loopers.application.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("OrderEventListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @InjectMocks
    private OrderEventListener listener;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    @DisplayName("OrderCompletedEvent 수신 시 UserActionEvent(ORDER)를 발행한다")
    void publishesUserActionEvent_whenOrderCompleted() {
        // Arrange
        OrderCompletedEvent event = new OrderCompletedEvent(
            1L, 10L, Set.of(100L, 200L), 50000L
        );

        // Act
        listener.handleOrderCompleted(event);

        // Assert
        ArgumentCaptor<UserActionEvent> captor = ArgumentCaptor.forClass(UserActionEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        UserActionEvent actionEvent = captor.getValue();
        assertThat(actionEvent.memberId()).isEqualTo(10L);
        assertThat(actionEvent.actionType()).isEqualTo(ActionType.ORDER);
        assertThat(actionEvent.targetId()).isEqualTo(1L);
        assertThat(actionEvent.targetType()).isEqualTo("ORDER");
        assertThat(actionEvent.metadata()).contains("50000");
    }
}
