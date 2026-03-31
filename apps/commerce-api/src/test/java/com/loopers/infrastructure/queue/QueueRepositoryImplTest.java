package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueRepository;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("QueueRepositoryImpl 통합 테스트")
class QueueRepositoryImplTest {

    @Autowired
    private QueueRepository queueRepository;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private static final String EVENT_TYPE = "order";

    @BeforeEach
    void setUp() {
        redisCleanUp.truncateAll();
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @DisplayName("addIfAbsent")
    @Nested
    class AddIfAbsent {

        @Test
        @DisplayName("새로운 유저를 추가하면 true를 반환한다")
        void returnsTrue_whenNewUser() {
            // act
            boolean result = queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);

            // assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("이미 존재하는 유저를 추가하면 false를 반환한다")
        void returnsFalse_whenUserAlreadyExists() {
            // arrange
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);

            // act
            boolean result = queueRepository.addIfAbsent(EVENT_TYPE, 1L, 2000.0);

            // assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("이미 존재하는 유저의 score는 변경되지 않는다")
        void doesNotUpdateScore_whenUserAlreadyExists() {
            // arrange
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 2L, 2000.0);

            // act
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 3000.0);

            // assert — userId 1의 순번이 여전히 1번 (score 1000)
            assertThat(queueRepository.getPosition(EVENT_TYPE, 1L)).isEqualTo(1L);
        }
    }

    @DisplayName("getPosition")
    @Nested
    class GetPosition {

        @Test
        @DisplayName("score 순서대로 순번을 반환한다 (1-based)")
        void returnsPositionByScoreOrder() {
            // arrange
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 2L, 2000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 3L, 3000.0);

            // act & assert
            assertThat(queueRepository.getPosition(EVENT_TYPE, 1L)).isEqualTo(1L);
            assertThat(queueRepository.getPosition(EVENT_TYPE, 2L)).isEqualTo(2L);
            assertThat(queueRepository.getPosition(EVENT_TYPE, 3L)).isEqualTo(3L);
        }

        @Test
        @DisplayName("존재하지 않는 유저 조회 시 null을 반환한다")
        void returnsNull_whenUserNotFound() {
            // act
            Long position = queueRepository.getPosition(EVENT_TYPE, 999L);

            // assert
            assertThat(position).isNull();
        }
    }

    @DisplayName("getTotalWaiting")
    @Nested
    class GetTotalWaiting {

        @Test
        @DisplayName("대기열의 전체 인원 수를 반환한다")
        void returnsTotalCount() {
            // arrange
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 2L, 2000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 3L, 3000.0);

            // act
            long total = queueRepository.getTotalWaiting(EVENT_TYPE);

            // assert
            assertThat(total).isEqualTo(3L);
        }

        @Test
        @DisplayName("빈 대기열의 인원 수는 0이다")
        void returnsZero_whenEmpty() {
            // act
            long total = queueRepository.getTotalWaiting(EVENT_TYPE);

            // assert
            assertThat(total).isEqualTo(0L);
        }

        @Test
        @DisplayName("서로 다른 eventType은 독립적으로 카운트된다")
        void countsIndependently_forDifferentEventTypes() {
            // arrange
            queueRepository.addIfAbsent("order", 1L, 1000.0);
            queueRepository.addIfAbsent("order", 2L, 2000.0);
            queueRepository.addIfAbsent("flash-sale", 1L, 1000.0);

            // act & assert
            assertThat(queueRepository.getTotalWaiting("order")).isEqualTo(2L);
            assertThat(queueRepository.getTotalWaiting("flash-sale")).isEqualTo(1L);
        }
    }

    @DisplayName("popMin")
    @Nested
    class PopMin {

        @Test
        @DisplayName("score가 낮은 순서대로 N명을 추출한다")
        void popsLowestScoreMembers() {
            // arrange
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 2L, 2000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 3L, 3000.0);

            // act
            List<Long> popped = queueRepository.popMin(EVENT_TYPE, 2);

            // assert
            assertThat(popped).containsExactly(1L, 2L);
            assertThat(queueRepository.getTotalWaiting(EVENT_TYPE)).isEqualTo(1L);
            assertThat(queueRepository.getPosition(EVENT_TYPE, 3L)).isEqualTo(1L);
        }

        @Test
        @DisplayName("빈 대기열에서 popMin 시 빈 리스트를 반환한다")
        void returnsEmptyList_whenQueueEmpty() {
            // act
            List<Long> popped = queueRepository.popMin(EVENT_TYPE, 5);

            // assert
            assertThat(popped).isEmpty();
        }

        @Test
        @DisplayName("대기 인원보다 큰 count로 popMin 시 전원 추출한다")
        void popsAll_whenCountExceedsSize() {
            // arrange
            queueRepository.addIfAbsent(EVENT_TYPE, 1L, 1000.0);
            queueRepository.addIfAbsent(EVENT_TYPE, 2L, 2000.0);

            // act
            List<Long> popped = queueRepository.popMin(EVENT_TYPE, 10);

            // assert
            assertThat(popped).containsExactly(1L, 2L);
            assertThat(queueRepository.getTotalWaiting(EVENT_TYPE)).isEqualTo(0L);
        }
    }
}
