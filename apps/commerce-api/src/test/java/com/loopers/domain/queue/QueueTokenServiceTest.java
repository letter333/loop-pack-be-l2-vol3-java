package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.loopers.domain.queue.TokenConsumeResult.ConsumeStatus;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QueueTokenServiceTest {

    @Mock
    private QueueTokenRepository queueTokenRepository;

    private QueueTokenService queueTokenService;

    private static final String EVENT_TYPE = "order";
    private static final Long USER_ID = 1L;
    private static final long TTL_MINUTES = 5;

    @BeforeEach
    void setUp() {
        queueTokenService = new QueueTokenService(queueTokenRepository, TTL_MINUTES);
    }

    @DisplayName("토큰 발급")
    @Nested
    class IssueToken {

        @Test
        @DisplayName("UUID 토큰을 생성하고 저장한다")
        void issuesUuidTokenAndSaves() {
            // act
            String token = queueTokenService.issueToken(EVENT_TYPE, USER_ID);

            // assert
            assertThat(token).isNotNull().isNotEmpty();
            verify(queueTokenRepository).save(eq(EVENT_TYPE), eq(USER_ID), eq(token), eq(Duration.ofMinutes(TTL_MINUTES)));
        }
    }

    @DisplayName("토큰 검증 및 소비")
    @Nested
    class ValidateAndConsume {

        @Test
        @DisplayName("유효한 토큰이면 잔여 TTL을 반환한다")
        void returnsRemainingTtl_whenTokenValid() {
            // arrange
            String token = "valid-token";
            given(queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, token))
                .willReturn(new TokenConsumeResult(ConsumeStatus.CONSUMED, 180L));

            // act
            long ttl = queueTokenService.validateAndConsume(EVENT_TYPE, USER_ID, token);

            // assert
            assertThat(ttl).isEqualTo(180L);
        }

        @Test
        @DisplayName("토큰이 없으면 UNAUTHORIZED 예외가 발생한다")
        void throwsUnauthorized_whenNoToken() {
            // arrange
            String token = "any-token";
            given(queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, token))
                .willReturn(new TokenConsumeResult(ConsumeStatus.NOT_FOUND, 0));

            // act & assert
            assertThatThrownBy(() -> queueTokenService.validateAndConsume(EVENT_TYPE, USER_ID, token))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED));
        }

        @Test
        @DisplayName("토큰이 일치하지 않으면 UNAUTHORIZED 예외가 발생한다")
        void throwsUnauthorized_whenTokenMismatch() {
            // arrange
            String token = "wrong-token";
            given(queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, token))
                .willReturn(new TokenConsumeResult(ConsumeStatus.MISMATCH, 0));

            // act & assert
            assertThatThrownBy(() -> queueTokenService.validateAndConsume(EVENT_TYPE, USER_ID, token))
                .isInstanceOf(CoreException.class)
                .satisfies(e -> assertThat(((CoreException) e).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED));
        }
    }

    @DisplayName("토큰 복원")
    @Nested
    class RestoreToken {

        @Test
        @DisplayName("잔여 TTL이 양수이면 토큰을 복원한다")
        void restoresToken_whenTtlPositive() {
            // act
            queueTokenService.restoreToken(EVENT_TYPE, USER_ID, "token", 120L);

            // assert
            verify(queueTokenRepository).save(EVENT_TYPE, USER_ID, "token", Duration.ofSeconds(120));
        }

        @Test
        @DisplayName("잔여 TTL이 0 이하이면 복원하지 않는다")
        void doesNotRestore_whenTtlZeroOrNegative() {
            // act
            queueTokenService.restoreToken(EVENT_TYPE, USER_ID, "token", 0L);

            // assert
            verify(queueTokenRepository, never()).save(anyString(), any(), anyString(), any());
        }
    }
}
