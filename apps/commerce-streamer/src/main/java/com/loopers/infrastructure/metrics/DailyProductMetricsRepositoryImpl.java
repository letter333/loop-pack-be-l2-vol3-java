package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.DailyProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class DailyProductMetricsRepositoryImpl implements DailyProductMetricsRepository {

    private final DailyProductMetricsJpaRepository dailyProductMetricsJpaRepository;

    @Override
    public void incrementLikeCount(Long productId, long delta, LocalDate metricDate) {
        dailyProductMetricsJpaRepository.incrementLikeCount(productId, delta, metricDate);
    }

    @Override
    public void incrementViewCount(Long productId, long delta, LocalDate metricDate) {
        dailyProductMetricsJpaRepository.incrementViewCount(productId, delta, metricDate);
    }

    @Override
    public void incrementSalesCount(Long productId, long count, long amount, LocalDate metricDate) {
        dailyProductMetricsJpaRepository.incrementSalesCount(productId, count, amount, metricDate);
    }
}
