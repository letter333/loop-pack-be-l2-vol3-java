package com.loopers.application.product;

import java.util.Optional;

public interface ProductDetailCacheRepository {
    Optional<ProductDetailInfo> get(Long productId);
    void put(Long productId, ProductDetailInfo productDetailInfo);
    void evict(Long productId);
}
