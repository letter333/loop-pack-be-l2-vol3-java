package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @Transactional: @Modifying 네이티브 쿼리가 활성 트랜잭션을 필요로 함.
 * 운영 환경에서는 MetricsService.processEvent()의 @Transactional이 이 역할을 수행.
 * Spring Test의 자동 롤백으로 테스트 간 데이터 격리 보장.
 */
@SpringBootTest
@Transactional
@DisplayName("ProductMetricsRepositoryImpl 통합 테스트")
class ProductMetricsRepositoryImplIntegrationTest {

    @Autowired
    private ProductMetricsRepository productMetricsRepository;

    @Test
    @DisplayName("incrementLikeCount 호출 시 누적값이 증가한다")
    void incrementsLikeCountCumulatively() {
        // Arrange & Act
        productMetricsRepository.incrementLikeCount(1L, 3);
        productMetricsRepository.incrementLikeCount(1L, 2);

        // Assert
        Optional<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
        assertThat(metrics).isPresent();
        assertThat(metrics.get().getLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("incrementViewCount 호출 시 누적값이 증가한다")
    void incrementsViewCountCumulatively() {
        // Arrange & Act
        productMetricsRepository.incrementViewCount(1L, 10);

        // Assert
        Optional<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
        assertThat(metrics).isPresent();
        assertThat(metrics.get().getViewCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("incrementSalesCount 호출 시 salesCount와 salesAmount가 누적된다")
    void incrementsSalesCountCumulatively() {
        // Arrange & Act
        productMetricsRepository.incrementSalesCount(1L, 1, 50000);
        productMetricsRepository.incrementSalesCount(1L, 2, 30000);

        // Assert
        Optional<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
        assertThat(metrics).isPresent();
        assertThat(metrics.get().getSalesCount()).isEqualTo(3L);
        assertThat(metrics.get().getSalesAmount()).isEqualTo(80000L);
    }
}
