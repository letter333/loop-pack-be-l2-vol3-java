package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.DailyProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DailyProductMetricsRepositoryImpl implements DailyProductMetricsRepository {

    private final DailyProductMetricsJpaRepository dailyProductMetricsJpaRepository;

    @Override
    public void incrementLikeCount(Long productId, long delta) {
        dailyProductMetricsJpaRepository.incrementLikeCount(productId, delta);
    }

    @Override
    public void incrementViewCount(Long productId, long delta) {
        dailyProductMetricsJpaRepository.incrementViewCount(productId, delta);
    }

    @Override
    public void incrementSalesCount(Long productId, long count, long amount) {
        dailyProductMetricsJpaRepository.incrementSalesCount(productId, count, amount);
    }
}
