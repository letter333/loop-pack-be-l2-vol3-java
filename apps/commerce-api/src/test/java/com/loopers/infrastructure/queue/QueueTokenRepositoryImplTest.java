package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueTokenRepository;
import com.loopers.domain.queue.TokenConsumeResult;
import com.loopers.domain.queue.TokenConsumeResult.ConsumeStatus;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("QueueTokenRepositoryImpl 통합 테스트")
class QueueTokenRepositoryImplTest {

    @Autowired
    private QueueTokenRepository queueTokenRepository;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private static final String EVENT_TYPE = "order";
    private static final Long USER_ID = 1L;
    private static final String TOKEN = "test-token-uuid";

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("save + get")
    @Nested
    class SaveAndGet {

        @Test
        @DisplayName("저장한 토큰을 조회할 수 있다")
        void returnsToken_afterSave() {
            // act
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // assert
            String result = queueTokenRepository.get(EVENT_TYPE, USER_ID);
            assertThat(result).isEqualTo(TOKEN);
        }

        @Test
        @DisplayName("존재하지 않는 토큰 조회 시 null을 반환한다")
        void returnsNull_whenNotExists() {
            // act
            String result = queueTokenRepository.get(EVENT_TYPE, 999L);

            // assert
            assertThat(result).isNull();
        }
    }

    @DisplayName("consumeIfMatches (Lua Script 원자적 토큰 검증 + 삭제)")
    @Nested
    class ConsumeIfMatches {

        @Test
        @DisplayName("토큰이 일치하면 CONSUMED 상태와 잔여 TTL을 반환하고 키를 삭제한다")
        void returnsConsumedWithTtl_whenTokenMatches() {
            // arrange
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // act
            TokenConsumeResult result = queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, TOKEN);

            // assert
            assertThat(result.status()).isEqualTo(ConsumeStatus.CONSUMED);
            assertThat(result.ttlSeconds()).isBetween(295L, 300L);
            assertThat(queueTokenRepository.get(EVENT_TYPE, USER_ID)).isNull();
        }

        @Test
        @DisplayName("토큰이 일치하지 않으면 MISMATCH 상태를 반환하고 키를 삭제하지 않는다")
        void returnsMismatch_whenTokenDoesNotMatch() {
            // arrange
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // act
            TokenConsumeResult result = queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, "wrong-token");

            // assert
            assertThat(result.status()).isEqualTo(ConsumeStatus.MISMATCH);
            assertThat(queueTokenRepository.get(EVENT_TYPE, USER_ID)).isEqualTo(TOKEN);
        }

        @Test
        @DisplayName("두 번 호출 시 첫 번째만 CONSUMED를 반환한다 (원자성)")
        void returnsNotFoundOnSecondCall() {
            // arrange
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // act
            TokenConsumeResult first = queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, TOKEN);
            TokenConsumeResult second = queueTokenRepository.consumeIfMatches(EVENT_TYPE, USER_ID, TOKEN);

            // assert
            assertThat(first.status()).isEqualTo(ConsumeStatus.CONSUMED);
            assertThat(second.status()).isEqualTo(ConsumeStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 키에 대해 NOT_FOUND 상태를 반환한다")
        void returnsNotFound_whenNotExists() {
            // act
            TokenConsumeResult result = queueTokenRepository.consumeIfMatches(EVENT_TYPE, 999L, TOKEN);

            // assert
            assertThat(result.status()).isEqualTo(ConsumeStatus.NOT_FOUND);
            assertThat(result.ttlSeconds()).isEqualTo(0L);
        }
    }

    @DisplayName("eventType 독립성")
    @Nested
    class EventTypeIndependence {

        @Test
        @DisplayName("서로 다른 eventType은 독립적으로 관리된다")
        void tokensSeparatedByEventType() {
            // arrange
            queueTokenRepository.save("order", USER_ID, "order-token", Duration.ofMinutes(5));
            queueTokenRepository.save("flash-sale", USER_ID, "sale-token", Duration.ofMinutes(5));

            // act & assert
            assertThat(queueTokenRepository.get("order", USER_ID)).isEqualTo("order-token");
            assertThat(queueTokenRepository.get("flash-sale", USER_ID)).isEqualTo("sale-token");
        }
    }
}
