package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Nested
    @DisplayName("incrementLikeCount")
    class IncrementLikeCount {

        @Test
        @DisplayName("첫 호출 시 새로운 레코드가 생성된다")
        void createsNewRecordOnFirstCall() {
            // Arrange & Act
            productMetricsRepository.incrementLikeCount(1L, 1);

            // Assert
            List<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
            assertThat(metrics).hasSize(1);
            assertThat(metrics.get(0).getLikeCount()).isEqualTo(1L);
            assertThat(metrics.get(0).getMetricDate()).isNotNull();
        }

        @Test
        @DisplayName("같은 날 같은 상품에 두 번 호출하면 값이 누적된다")
        void accumulatesOnSameDaySameProduct() {
            // Arrange
            productMetricsRepository.incrementLikeCount(1L, 3);

            // Act
            productMetricsRepository.incrementLikeCount(1L, 2);

            // Assert
            List<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
            assertThat(metrics).hasSize(1);
            assertThat(metrics.get(0).getLikeCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("다른 상품의 메트릭은 독립적으로 관리된다")
        void differentProductsAreIndependent() {
            // Arrange & Act
            productMetricsRepository.incrementLikeCount(1L, 10);
            productMetricsRepository.incrementLikeCount(2L, 20);

            // Assert
            List<ProductMetrics> metrics1 = productMetricsRepository.findByProductId(1L);
            List<ProductMetrics> metrics2 = productMetricsRepository.findByProductId(2L);
            assertThat(metrics1.get(0).getLikeCount()).isEqualTo(10L);
            assertThat(metrics2.get(0).getLikeCount()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("incrementViewCount")
    class IncrementViewCount {

        @Test
        @DisplayName("viewCount를 증가시킨다")
        void incrementsViewCount() {
            // Arrange & Act
            productMetricsRepository.incrementViewCount(1L, 5);

            // Assert
            List<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
            assertThat(metrics).hasSize(1);
            assertThat(metrics.get(0).getViewCount()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("incrementSalesCount")
    class IncrementSalesCount {

        @Test
        @DisplayName("salesCount와 salesAmount를 증가시킨다")
        void incrementsSalesCountAndAmount() {
            // Arrange & Act
            productMetricsRepository.incrementSalesCount(1L, 1, 50000);

            // Assert
            List<ProductMetrics> metrics = productMetricsRepository.findByProductId(1L);
            assertThat(metrics).hasSize(1);
            assertThat(metrics.get(0).getSalesCount()).isEqualTo(1L);
            assertThat(metrics.get(0).getSalesAmount()).isEqualTo(50000L);
        }
    }
}
