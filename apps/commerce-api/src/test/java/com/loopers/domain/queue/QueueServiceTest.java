package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class QueueServiceTest {

    @Mock
    private QueueRepository queueRepository;

    @InjectMocks
    private QueueService queueService;

    private static final String EVENT_TYPE = "order";
    private static final Long USER_ID = 1L;

    @DisplayName("대기열 진입")
    @Nested
    class Enter {

        @Test
        @DisplayName("정상 진입 시 순번과 전체 대기 인원을 반환한다")
        void returnsQueueInfo_whenEnterSuccessfully() {
            // arrange
            given(queueRepository.addIfAbsent(eq(EVENT_TYPE), eq(USER_ID), anyDouble())).willReturn(true);
            given(queueRepository.getPosition(EVENT_TYPE, USER_ID)).willReturn(1L);
            given(queueRepository.getTotalWaiting(EVENT_TYPE)).willReturn(1L);

            // act
            QueueInfo result = queueService.enter(EVENT_TYPE, USER_ID);

            // assert
            assertAll(
                () -> assertThat(result.position()).isEqualTo(1L),
                () -> assertThat(result.totalWaiting()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("이미 대기열에 있는 유저가 진입하면 CONFLICT 예외가 발생한다")
        void throwsConflict_whenAlreadyInQueue() {
            // arrange
            given(queueRepository.addIfAbsent(eq(EVENT_TYPE), eq(USER_ID), anyDouble())).willReturn(false);

            // act & assert
            assertThatThrownBy(() -> queueService.enter(EVENT_TYPE, USER_ID))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.CONFLICT));
        }
    }

    @DisplayName("대기열 정보 조회")
    @Nested
    class GetQueueInfo {

        @Test
        @DisplayName("대기열에 있는 유저의 순번과 대기 인원을 반환한다")
        void returnsQueueInfo_whenUserInQueue() {
            // arrange
            given(queueRepository.getPosition(EVENT_TYPE, USER_ID)).willReturn(5L);
            given(queueRepository.getTotalWaiting(EVENT_TYPE)).willReturn(10L);

            // act
            QueueInfo result = queueService.getQueueInfo(EVENT_TYPE, USER_ID);

            // assert
            assertAll(
                () -> assertThat(result.position()).isEqualTo(5L),
                () -> assertThat(result.totalWaiting()).isEqualTo(10L)
            );
        }

        @Test
        @DisplayName("대기열에 없는 유저를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenUserNotInQueue() {
            // arrange
            given(queueRepository.getPosition(EVENT_TYPE, USER_ID)).willReturn(null);

            // act & assert
            assertThatThrownBy(() -> queueService.getQueueInfo(EVENT_TYPE, USER_ID))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }
}
