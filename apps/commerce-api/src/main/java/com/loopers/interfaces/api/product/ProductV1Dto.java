package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.ProductStatus;

public class ProductV1Dto {

    public record BrandResponse(
        Long id,
        String name,
        String logoImageUrl
    ) {
        public static BrandResponse from(com.loopers.application.brand.BrandInfo info) {
            return new BrandResponse(
                info.id(),
                info.name(),
                info.logoImageUrl()
            );
        }
    }

    public record ProductResponse(
        Long id,
        String name,
        String productCode,
        Long basePrice,
        Long discountedPrice,
        ProductStatus status,
        Long discount,
        DiscountType discountType,
        BrandResponse brand,
        Long likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.name(),
                info.productCode(),
                info.basePrice(),
                info.discountedPrice(),
                info.status(),
                info.discount(),
                info.discountType(),
                info.brand() != null ? BrandResponse.from(info.brand()) : null,
                info.likeCount()
            );
        }
    }
}