package com.loopers.application.event;

import com.loopers.domain.actionlog.UserActionLog;
import com.loopers.domain.actionlog.UserActionLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("UserActionEventListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class UserActionEventListenerTest {

    private UserActionEventListener listener;

    @Mock
    private UserActionLogRepository userActionLogRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        listener = new UserActionEventListener(userActionLogRepository, transactionManager);
    }

    @Test
    @DisplayName("UserActionEvent 수신 시 UserActionLog를 저장한다")
    void savesUserActionLog_whenEventReceived() {
        // Arrange
        UserActionEvent event = new UserActionEvent(
            1L, ActionType.VIEW, 100L, "PRODUCT", null
        );

        // Act
        listener.handleUserAction(event);

        // Assert
        ArgumentCaptor<UserActionLog> captor = ArgumentCaptor.forClass(UserActionLog.class);
        verify(userActionLogRepository).save(captor.capture());

        UserActionLog savedLog = captor.getValue();
        assertThat(savedLog.getMemberId()).isEqualTo(1L);
        assertThat(savedLog.getActionType()).isEqualTo("VIEW");
        assertThat(savedLog.getTargetId()).isEqualTo(100L);
        assertThat(savedLog.getTargetType()).isEqualTo("PRODUCT");
        assertThat(savedLog.getMetadata()).isNull();
    }

    @Test
    @DisplayName("메타데이터가 포함된 이벤트를 처리한다")
    void savesUserActionLog_withMetadata() {
        // Arrange
        UserActionEvent event = new UserActionEvent(
            2L, ActionType.ORDER, 200L, "ORDER", "{\"totalAmount\":50000}"
        );

        // Act
        listener.handleUserAction(event);

        // Assert
        ArgumentCaptor<UserActionLog> captor = ArgumentCaptor.forClass(UserActionLog.class);
        verify(userActionLogRepository).save(captor.capture());

        UserActionLog savedLog = captor.getValue();
        assertThat(savedLog.getMemberId()).isEqualTo(2L);
        assertThat(savedLog.getActionType()).isEqualTo("ORDER");
        assertThat(savedLog.getTargetId()).isEqualTo(200L);
        assertThat(savedLog.getTargetType()).isEqualTo("ORDER");
        assertThat(savedLog.getMetadata()).isEqualTo("{\"totalAmount\":50000}");
    }

    @Test
    @DisplayName("LIKE 액션 이벤트를 처리한다")
    void savesUserActionLog_forLikeAction() {
        // Arrange
        UserActionEvent event = new UserActionEvent(
            3L, ActionType.LIKE, 300L, "PRODUCT", null
        );

        // Act
        listener.handleUserAction(event);

        // Assert
        ArgumentCaptor<UserActionLog> captor = ArgumentCaptor.forClass(UserActionLog.class);
        verify(userActionLogRepository).save(captor.capture());

        UserActionLog savedLog = captor.getValue();
        assertThat(savedLog.getMemberId()).isEqualTo(3L);
        assertThat(savedLog.getActionType()).isEqualTo("LIKE");
        assertThat(savedLog.getTargetId()).isEqualTo(300L);
        assertThat(savedLog.getTargetType()).isEqualTo("PRODUCT");
    }

    @Test
    @DisplayName("PAYMENT 액션 이벤트를 처리한다")
    void savesUserActionLog_forPaymentAction() {
        // Arrange
        UserActionEvent event = new UserActionEvent(
            4L, ActionType.PAYMENT, 400L, "PAYMENT", "{\"amount\":30000}"
        );

        // Act
        listener.handleUserAction(event);

        // Assert
        ArgumentCaptor<UserActionLog> captor = ArgumentCaptor.forClass(UserActionLog.class);
        verify(userActionLogRepository).save(captor.capture());

        UserActionLog savedLog = captor.getValue();
        assertThat(savedLog.getMemberId()).isEqualTo(4L);
        assertThat(savedLog.getActionType()).isEqualTo("PAYMENT");
        assertThat(savedLog.getTargetId()).isEqualTo(400L);
        assertThat(savedLog.getMetadata()).isEqualTo("{\"amount\":30000}");
    }
}
