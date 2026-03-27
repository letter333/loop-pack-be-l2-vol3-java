package com.loopers.application.event;

import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductLikeCountListener {

    private final ProductService productService;

    @EventListener
    public void handleProductLiked(ProductLikedEvent event) {
        productService.increaseLikeCount(event.productId());
    }

    @EventListener
    public void handleProductUnliked(ProductUnlikedEvent event) {
        productService.decreaseLikeCount(event.productId());
    }
}
