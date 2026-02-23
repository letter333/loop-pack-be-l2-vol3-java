package com.loopers.application.product;

import com.loopers.domain.product.ImageType;
import com.loopers.domain.product.ProductImage;

public record ProductImageInfo(
    Long id,
    ImageType type,
    String url,
    String altText
) {
    public static ProductImageInfo from(ProductImage image) {
        return new ProductImageInfo(
            image.getId(),
            image.getType(),
            image.getUrl(),
            image.getAltText()
        );
    }
}
