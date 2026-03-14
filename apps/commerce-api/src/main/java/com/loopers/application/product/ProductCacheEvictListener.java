package com.loopers.application.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProductCacheEvictListener {

    private final ProductDetailCacheRepository productDetailCacheRepository;
    private final ProductListCacheRepository productListCacheRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheEvict(ProductCacheEvictEvent event) {
        event.productIds().forEach(productDetailCacheRepository::evict);
        if (event.evictList()) {
            productListCacheRepository.evictAll();
        }
    }
}
