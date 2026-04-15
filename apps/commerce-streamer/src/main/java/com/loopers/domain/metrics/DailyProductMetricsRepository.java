package com.loopers.domain.metrics;

public interface DailyProductMetricsRepository {

    void incrementLikeCount(Long productId, long delta);

    void incrementViewCount(Long productId, long delta);

    void incrementSalesCount(Long productId, long count, long amount);
}
