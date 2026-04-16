package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.DailyProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class DailyProductMetricsRepositoryImpl implements DailyProductMetricsRepository {

    private final DailyProductMetricsJpaRepository dailyProductMetricsJpaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementLikeCount(Long productId, long delta) {
        dailyProductMetricsJpaRepository.incrementLikeCount(productId, delta);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementViewCount(Long productId, long delta) {
        dailyProductMetricsJpaRepository.incrementViewCount(productId, delta);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementSalesCount(Long productId, long count, long amount) {
        dailyProductMetricsJpaRepository.incrementSalesCount(productId, count, amount);
    }
}
