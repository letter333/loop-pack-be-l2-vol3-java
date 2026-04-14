package com.loopers.domain.metrics;

import java.util.List;

public interface ProductMetricsRepository {

    List<ProductMetrics> findByProductId(Long productId);

    ProductMetrics save(ProductMetrics metrics);

    void incrementLikeCount(Long productId, long delta);

    void incrementViewCount(Long productId, long delta);

    void incrementSalesCount(Long productId, long count, long amount);
}
