package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.DailyProductMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Transactional: @Modifying 네이티브 쿼리가 활성 트랜잭션을 필요로 함.
 * Spring Test의 자동 롤백으로 테스트 간 데이터 격리 보장.
 */
@SpringBootTest
@Transactional
@DisplayName("DailyProductMetricsRepositoryImpl 통합 테스트")
class DailyProductMetricsRepositoryImplIntegrationTest {

    @Autowired
    private DailyProductMetricsRepository dailyProductMetricsRepository;

    @Autowired
    private DailyProductMetricsJpaRepository dailyProductMetricsJpaRepository;

    private static final LocalDate TODAY = LocalDate.now();

    @Test
    @DisplayName("같은 날 같은 상품에 incrementLikeCount를 두 번 호출하면 값이 누적된다")
    void accumulatesLikeCountOnSameDay() {
        // Arrange & Act
        dailyProductMetricsRepository.incrementLikeCount(1L, 3, TODAY);
        dailyProductMetricsRepository.incrementLikeCount(1L, 2, TODAY);

        // Assert
        var entities = dailyProductMetricsJpaRepository.findAll();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getLikeCount()).isEqualTo(5L);
        assertThat(entities.get(0).getMetricDate()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("다른 상품의 일별 메트릭은 독립적으로 관리된다")
    void differentProductsAreIndependent() {
        // Arrange & Act
        dailyProductMetricsRepository.incrementViewCount(1L, 10, TODAY);
        dailyProductMetricsRepository.incrementViewCount(2L, 20, TODAY);

        // Assert
        var entities = dailyProductMetricsJpaRepository.findAll();
        assertThat(entities).hasSize(2);
    }

    @Test
    @DisplayName("incrementSalesCount가 salesCount와 salesAmount를 일별로 누적한다")
    void incrementsSalesCountDaily() {
        // Arrange & Act
        dailyProductMetricsRepository.incrementSalesCount(1L, 1, 50000, TODAY);

        // Assert
        var entities = dailyProductMetricsJpaRepository.findAll();
        assertThat(entities).hasSize(1);
        assertThat(entities.get(0).getSalesCount()).isEqualTo(1L);
        assertThat(entities.get(0).getSalesAmount()).isEqualTo(50000L);
    }

    @Test
    @DisplayName("다른 날짜는 별도 레코드로 저장된다")
    void differentDatesCreateSeparateRecords() {
        // Arrange & Act
        LocalDate yesterday = TODAY.minusDays(1);
        dailyProductMetricsRepository.incrementLikeCount(1L, 5, TODAY);
        dailyProductMetricsRepository.incrementLikeCount(1L, 3, yesterday);

        // Assert
        var entities = dailyProductMetricsJpaRepository.findAll();
        assertThat(entities).hasSize(2);
    }
}
