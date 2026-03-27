package com.loopers.domain.metrics;

import java.util.Optional;

public interface ProductMetricsRepository {

    Optional<ProductMetrics> findByProductId(Long productId);

    ProductMetrics save(ProductMetrics metrics);

    void incrementLikeCount(Long productId, long delta);

    void incrementViewCount(Long productId, long delta);

    void incrementSalesCount(Long productId, long count, long amount);
}
