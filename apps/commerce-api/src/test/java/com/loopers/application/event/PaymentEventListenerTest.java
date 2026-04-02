package com.loopers.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.actionlog.UserActionLog;
import com.loopers.domain.actionlog.UserActionLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("PaymentEventListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private UserActionLogRepository userActionLogRepository;

    @Test
    @DisplayName("PaymentSuccessEvent 수신 시 UserActionLog(PAYMENT)를 직접 저장한다")
    void savesUserActionLog_whenPaymentSucceeds() {
        // Arrange
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(mock(TransactionStatus.class));

        PaymentEventListener listener = new PaymentEventListener(
            userActionLogRepository, new ObjectMapper(), txManager
        );
        PaymentSuccessEvent event = new PaymentSuccessEvent(1L, 10L, 20L, 30000L);

        // Act
        listener.handlePaymentSuccess(event);

        // Assert
        ArgumentCaptor<UserActionLog> captor = ArgumentCaptor.forClass(UserActionLog.class);
        verify(userActionLogRepository).save(captor.capture());

        UserActionLog log = captor.getValue();
        assertThat(log.getMemberId()).isEqualTo(20L);
        assertThat(log.getActionType()).isEqualTo("PAYMENT");
        assertThat(log.getTargetId()).isEqualTo(1L);
        assertThat(log.getTargetType()).isEqualTo("PAYMENT");
        assertThat(log.getMetadata()).contains("30000");
        assertThat(log.getMetadata()).contains("10");
    }
}
