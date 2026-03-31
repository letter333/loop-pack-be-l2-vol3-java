package com.loopers.application.queue;

import com.loopers.domain.queue.QueueService;
import com.loopers.domain.queue.QueueTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueueAdmissionScheduler 단위 테스트")
class QueueAdmissionSchedulerTest {

    private final QueueService queueService = mock(QueueService.class);
    private final QueueTokenService queueTokenService = mock(QueueTokenService.class);
    private final QueueAdmissionScheduler scheduler = new QueueAdmissionScheduler(queueService, queueTokenService, 50);

    @Test
    @DisplayName("대기열에 유저가 있으면 각 유저에게 토큰을 발급한다")
    void issuesTokensForDequeuedUsers() {
        // arrange
        given(queueService.dequeueBatch("order", 50)).willReturn(List.of(1L, 2L, 3L));

        // act
        scheduler.processAdmission();

        // assert
        verify(queueTokenService).issueToken("order", 1L);
        verify(queueTokenService).issueToken("order", 2L);
        verify(queueTokenService).issueToken("order", 3L);
    }

    @Test
    @DisplayName("대기열이 비어있으면 토큰 발급을 하지 않는다")
    void doesNotIssueTokens_whenQueueEmpty() {
        // arrange
        given(queueService.dequeueBatch("order", 50)).willReturn(Collections.emptyList());

        // act
        scheduler.processAdmission();

        // assert
        verify(queueTokenService, never()).issueToken(anyString(), anyLong());
    }
}
