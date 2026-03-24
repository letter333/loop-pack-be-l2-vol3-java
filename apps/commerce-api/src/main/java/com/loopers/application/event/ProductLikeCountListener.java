package com.loopers.application.event;

import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.context.event.EventListener;

@Component
public class ProductLikeCountListener {

    private final ProductService productService;

    public ProductLikeCountListener(ProductService productService) {
        this.productService = productService;
    }

    @EventListener
    public void handleProductLiked(ProductLikedEvent event) {
        productService.increaseLikeCount(event.productId());
    }

    @EventListener
    public void handleProductUnliked(ProductUnlikedEvent event) {
        productService.decreaseLikeCount(event.productId());
    }
}
