package com.loopers.application;

import com.loopers.domain.ranking.RankingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZonedDateTime;

import static org.mockito.Mockito.verify;

@DisplayName("RankingService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RankingServiceTest {

    @InjectMocks
    private RankingService rankingService;

    @Mock
    private RankingRepository rankingRepository;

    private static final ZonedDateTime EVENT_CREATED_AT = ZonedDateTime.parse("2026-04-07T10:00:00+09:00");

    @Test
    @DisplayName("PRODUCT_VIEWED 이벤트 시 VIEW 가중치(0.1)로 점수를 누적한다")
    void addsViewScore() {
        // Act
        rankingService.addScore("PRODUCT_VIEWED", 1L, EVENT_CREATED_AT, 1.0);

        // Assert
        verify(rankingRepository).incrementScore("20260407", 1L, 0.1);
    }

    @Test
    @DisplayName("PRODUCT_LIKED 이벤트 시 LIKE 가중치(0.2)로 점수를 누적한다")
    void addsLikeScore() {
        // Act
        rankingService.addScore("PRODUCT_LIKED", 1L, EVENT_CREATED_AT, 1.0);

        // Assert
        verify(rankingRepository).incrementScore("20260407", 1L, 0.2);
    }

    @Test
    @DisplayName("PRODUCT_UNLIKED 이벤트 시 음수 점수로 감소한다")
    void subtractsUnlikeScore() {
        // Act
        rankingService.addScore("PRODUCT_UNLIKED", 1L, EVENT_CREATED_AT, -1.0);

        // Assert
        verify(rankingRepository).incrementScore("20260407", 1L, -0.2);
    }

    @Test
    @DisplayName("ORDER_COMPLETED 이벤트 시 ORDER 가중치(0.6)로 점수를 누적한다")
    void addsOrderScore() {
        // Act
        rankingService.addScore("ORDER_COMPLETED", 1L, EVENT_CREATED_AT, 50000.0);

        // Assert
        verify(rankingRepository).incrementScore("20260407", 1L, 30000.0);
    }

    @Test
    @DisplayName("이벤트 발생 날짜에 맞는 dateKey를 생성한다")
    void generatesCorrectDateKey() {
        // Arrange
        ZonedDateTime yesterday = ZonedDateTime.parse("2026-04-06T23:59:59+09:00");

        // Act
        rankingService.addScore("PRODUCT_VIEWED", 1L, yesterday, 1.0);

        // Assert
        verify(rankingRepository).incrementScore("20260406", 1L, 0.1);
    }
}
