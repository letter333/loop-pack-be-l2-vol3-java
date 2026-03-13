package com.loopers.application.product;

import java.util.Set;

public record ProductCacheEvictEvent(
    Set<Long> productIds,
    boolean evictList
) {
    public static ProductCacheEvictEvent listOnly() {
        return new ProductCacheEvictEvent(Set.of(), true);
    }

    public static ProductCacheEvictEvent of(Long productId) {
        return new ProductCacheEvictEvent(Set.of(productId), true);
    }

    public static ProductCacheEvictEvent of(Set<Long> productIds) {
        return new ProductCacheEvictEvent(productIds, true);
    }
}
