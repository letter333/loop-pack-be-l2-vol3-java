package com.loopers.application.product;

import com.loopers.domain.product.ProductSortType;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface ProductListCacheRepository {
    Optional<Page<ProductInfo>> get(String cacheKey);
    void put(String cacheKey, Page<ProductInfo> page);
    void evict(String cacheKey);
    void evictAll();
    String generateKey(Long categoryId, Long brandId, ProductSortType sort, int page, int size);
}
