package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueTokenRepository;
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

    @DisplayName("getAndDelete (GETDEL)")
    @Nested
    class GetAndDelete {

        @Test
        @DisplayName("토큰을 읽고 삭제한다")
        void returnsTokenAndDeletes() {
            // arrange
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // act
            String result = queueTokenRepository.getAndDelete(EVENT_TYPE, USER_ID);

            // assert
            assertThat(result).isEqualTo(TOKEN);
            assertThat(queueTokenRepository.get(EVENT_TYPE, USER_ID)).isNull();
        }

        @Test
        @DisplayName("두 번 호출 시 첫 번째만 값을 반환한다 (원자성)")
        void returnsNullOnSecondCall() {
            // arrange
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // act
            String first = queueTokenRepository.getAndDelete(EVENT_TYPE, USER_ID);
            String second = queueTokenRepository.getAndDelete(EVENT_TYPE, USER_ID);

            // assert
            assertThat(first).isEqualTo(TOKEN);
            assertThat(second).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 토큰에 대해 null을 반환한다")
        void returnsNull_whenNotExists() {
            // act
            String result = queueTokenRepository.getAndDelete(EVENT_TYPE, 999L);

            // assert
            assertThat(result).isNull();
        }
    }

    @DisplayName("getExpire (TTL)")
    @Nested
    class GetExpire {

        @Test
        @DisplayName("저장 시 설정한 TTL의 잔여 시간을 반환한다")
        void returnsTtlInSeconds() {
            // arrange
            queueTokenRepository.save(EVENT_TYPE, USER_ID, TOKEN, Duration.ofMinutes(5));

            // act
            Long ttl = queueTokenRepository.getExpire(EVENT_TYPE, USER_ID);

            // assert — 5분 = 300초, 저장 직후이므로 299~300초 사이
            assertThat(ttl).isNotNull();
            assertThat(ttl).isBetween(295L, 300L);
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
