package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;

    @Override
    public List<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId).stream()
            .map(ProductMetricsEntity::toDomain)
            .toList();
    }

    @Override
    public ProductMetrics save(ProductMetrics metrics) {
        return productMetricsJpaRepository.save(ProductMetricsEntity.from(metrics)).toDomain();
    }

    @Override
    public void incrementLikeCount(Long productId, long delta) {
        productMetricsJpaRepository.incrementLikeCount(productId, delta);
    }

    @Override
    public void incrementViewCount(Long productId, long delta) {
        productMetricsJpaRepository.incrementViewCount(productId, delta);
    }

    @Override
    public void incrementSalesCount(Long productId, long count, long amount) {
        productMetricsJpaRepository.incrementSalesCount(productId, count, amount);
    }
}
