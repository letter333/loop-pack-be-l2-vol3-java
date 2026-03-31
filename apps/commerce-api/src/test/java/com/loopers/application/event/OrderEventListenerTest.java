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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OrderEventListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class OrderEventListenerTest {

    @Mock
    private UserActionLogRepository userActionLogRepository;

    @Test
    @DisplayName("OrderCompletedEvent 수신 시 UserActionLog(ORDER)를 직접 저장한다")
    void savesUserActionLog_whenOrderCompleted() {
        // Arrange
        PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);
        when(txManager.getTransaction(any(TransactionDefinition.class)))
            .thenReturn(mock(TransactionStatus.class));

        OrderEventListener listener = new OrderEventListener(
            userActionLogRepository, new ObjectMapper(), txManager
        );
        OrderCompletedEvent event = new OrderCompletedEvent(1L, 10L, Set.of(100L, 200L), 50000L);

        // Act
        listener.handleOrderCompleted(event);

        // Assert
        ArgumentCaptor<UserActionLog> captor = ArgumentCaptor.forClass(UserActionLog.class);
        verify(userActionLogRepository).save(captor.capture());

        UserActionLog log = captor.getValue();
        assertThat(log.getMemberId()).isEqualTo(10L);
        assertThat(log.getActionType()).isEqualTo("ORDER");
        assertThat(log.getTargetId()).isEqualTo(1L);
        assertThat(log.getTargetType()).isEqualTo("ORDER");
        assertThat(log.getMetadata()).contains("50000");
    }
}
