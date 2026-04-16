package com.loopers.application.ranking;

import com.loopers.domain.ranking.ProductRankingRepository;
import com.loopers.domain.ranking.RankingDetailInfo;
import com.loopers.domain.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RankingFacade 단위 테스트")
@ExtendWith(MockitoExtension.class)
class RankingFacadeTest {

    @InjectMocks
    private RankingFacade rankingFacade;

    @Mock
    private ProductRankingRepository productRankingRepository;

    @Nested
    @DisplayName("getRankings")
    class GetRankings {

        @Test
        @DisplayName("DAILY 요청 시 일간 랭킹을 조회한다")
        void getDailyRankings() {
            // Arrange
            List<RankingInfo> expected = List.of(
                new RankingInfo(1L, 1, 100.0, 10L, 50L, 5L, 30000L)
            );
            when(productRankingRepository.getDailyRankings("20260414", 0, 20))
                .thenReturn(expected);

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.DAILY, "20260414", 1, 20);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).productId()).isEqualTo(1L);
            verify(productRankingRepository).getDailyRankings("20260414", 0, 20);
        }

        @Test
        @DisplayName("WEEKLY 요청 시 주간 랭킹을 조회한다")
        void getWeeklyRankings() {
            // Arrange
            when(productRankingRepository.getWeeklyRankings(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.WEEKLY, "20260414", 1, 20);

            // Assert
            assertThat(result).isEmpty();
            verify(productRankingRepository).getWeeklyRankings(anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("MONTHLY 요청 시 월간 랭킹을 조회한다")
        void getMonthlyRankings() {
            // Arrange
            when(productRankingRepository.getMonthlyRankings(anyString(), anyInt(), anyInt()))
                .thenReturn(List.of());

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.MONTHLY, "20260414", 1, 20);

            // Assert
            assertThat(result).isEmpty();
            verify(productRankingRepository).getMonthlyRankings(anyString(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("date가 null이면 오늘 날짜로 조회한다 (DAILY)")
        void usesTodayWhenDateIsNull_daily() {
            // Arrange
            when(productRankingRepository.getDailyRankings(anyString(), eq(0), eq(20)))
                .thenReturn(List.of());

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.DAILY, null, 1, 20);

            // Assert
            assertThat(result).isEmpty();
            verify(productRankingRepository).getDailyRankings(anyString(), eq(0), eq(20));
        }

        @Test
        @DisplayName("date가 null이면 오늘 날짜 기준 주간 랭킹을 조회한다")
        void usesTodayWhenDateIsNull_weekly() {
            // Arrange
            when(productRankingRepository.getWeeklyRankings(anyString(), eq(0), eq(20)))
                .thenReturn(List.of());

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.WEEKLY, null, 1, 20);

            // Assert
            assertThat(result).isEmpty();
            verify(productRankingRepository).getWeeklyRankings(anyString(), eq(0), eq(20));
        }

        @Test
        @DisplayName("date가 null이면 오늘 날짜 기준 월간 랭킹을 조회한다")
        void usesTodayWhenDateIsNull_monthly() {
            // Arrange
            when(productRankingRepository.getMonthlyRankings(anyString(), eq(0), eq(20)))
                .thenReturn(List.of());

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.MONTHLY, null, 1, 20);

            // Assert
            assertThat(result).isEmpty();
            verify(productRankingRepository).getMonthlyRankings(anyString(), eq(0), eq(20));
        }

        @Test
        @DisplayName("page 2 요청 시 cursor이 올바르게 계산된다")
        void calculatesOffsetCorrectly() {
            // Arrange
            when(productRankingRepository.getDailyRankings("20260414", 20, 20))
                .thenReturn(List.of());

            // Act
            List<RankingInfo> result = rankingFacade.getRankings(RankingPeriod.DAILY, "20260414", 2, 20);

            // Assert
            verify(productRankingRepository).getDailyRankings("20260414", 20, 20);
        }
    }

    @Nested
    @DisplayName("getProductRanking")
    class GetProductRanking {

        @Test
        @DisplayName("상품의 일간 랭킹 정보를 반환한다")
        void returnsRankingDetailInfo() {
            // Arrange
            when(productRankingRepository.getProductDailyRanking(anyString(), eq(1L)))
                .thenReturn(Optional.of(new RankingDetailInfo(5L, 85.3)));

            // Act
            RankingDetailInfo result = rankingFacade.getProductRanking(1L);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.rank()).isEqualTo(5L);
            assertThat(result.score()).isEqualTo(85.3);
        }

        @Test
        @DisplayName("랭킹 데이터가 없으면 null을 반환한다")
        void returnsNullWhenNoRanking() {
            // Arrange
            when(productRankingRepository.getProductDailyRanking(anyString(), eq(999L)))
                .thenReturn(Optional.empty());

            // Act
            RankingDetailInfo result = rankingFacade.getProductRanking(999L);

            // Assert
            assertThat(result).isNull();
        }
    }
}
