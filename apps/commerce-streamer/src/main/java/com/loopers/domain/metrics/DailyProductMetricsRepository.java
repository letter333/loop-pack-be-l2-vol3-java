package com.loopers.domain.metrics;

import java.time.LocalDate;

public interface DailyProductMetricsRepository {

    void incrementLikeCount(Long productId, long delta, LocalDate metricDate);

    void incrementViewCount(Long productId, long delta, LocalDate metricDate);

    void incrementSalesCount(Long productId, long count, long amount, LocalDate metricDate);
}
