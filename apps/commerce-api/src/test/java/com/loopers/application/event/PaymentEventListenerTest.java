package com.loopers.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@DisplayName("PaymentEventListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @InjectMocks
    private PaymentEventListener listener;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Spy
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("PaymentSuccessEvent 수신 시 UserActionEvent(PAYMENT)를 발행한다")
    void publishesUserActionEvent_whenPaymentSucceeds() {
        // Arrange
        PaymentSuccessEvent event = new PaymentSuccessEvent(
            1L, 10L, 20L, 30000L
        );

        // Act
        listener.handlePaymentSuccess(event);

        // Assert
        ArgumentCaptor<UserActionEvent> captor = ArgumentCaptor.forClass(UserActionEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());

        UserActionEvent actionEvent = captor.getValue();
        assertThat(actionEvent.memberId()).isEqualTo(20L);
        assertThat(actionEvent.actionType()).isEqualTo(ActionType.PAYMENT);
        assertThat(actionEvent.targetId()).isEqualTo(1L);
        assertThat(actionEvent.targetType()).isEqualTo("PAYMENT");
        assertThat(actionEvent.metadata()).contains("30000");
        assertThat(actionEvent.metadata()).contains("10");
    }
}
