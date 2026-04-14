package com.loopers.domain.ranking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProductMetricsAggregation 단위 테스트")
class ProductMetricsAggregationTest {

    @Test
    @DisplayName("가중치 점수를 올바르게 계산한다 (view*0.1 + like*0.2 + salesAmount*0.6)")
    void calculatesScoreCorrectly() {
        // Arrange
        long viewCount = 100;
        long likeCount = 50;
        long salesAmount = 10000;

        // Act
        ProductMetricsAggregation aggregation = new ProductMetricsAggregation(
            1L, likeCount, viewCount, 5L, salesAmount
        );

        // Assert
        // score = 100 * 0.1 + 50 * 0.2 + 10000 * 0.6 = 10 + 10 + 6000 = 6020
        assertThat(aggregation.getScore()).isEqualTo(6020.0);
    }

    @Test
    @DisplayName("모든 메트릭이 0이면 점수도 0이다")
    void scoreIsZeroWhenAllMetricsAreZero() {
        // Arrange & Act
        ProductMetricsAggregation aggregation = new ProductMetricsAggregation(
            1L, 0L, 0L, 0L, 0L
        );

        // Assert
        assertThat(aggregation.getScore()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("ProductRankMv로 올바르게 변환된다")
    void convertsToProductRankMv() {
        // Arrange
        ProductMetricsAggregation aggregation = new ProductMetricsAggregation(
            42L, 10L, 100L, 5L, 50000L
        );

        // Act
        ProductRankMv mv = ProductRankMv.from(aggregation, "2026-W16", 3);

        // Assert
        assertThat(mv.getProductId()).isEqualTo(42L);
        assertThat(mv.getPeriodKey()).isEqualTo("2026-W16");
        assertThat(mv.getRank()).isEqualTo(3);
        assertThat(mv.getLikeCount()).isEqualTo(10L);
        assertThat(mv.getViewCount()).isEqualTo(100L);
        assertThat(mv.getSalesCount()).isEqualTo(5L);
        assertThat(mv.getSalesAmount()).isEqualTo(50000L);
        // score = 100*0.1 + 10*0.2 + 50000*0.6 = 10 + 2 + 30000 = 30012
        assertThat(mv.getScore()).isEqualTo(30012.0);
    }
}
