package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

public record ProductInfo(
    Long id,
    String name,
    String productCode,
    Long basePrice,
    Long discountedPrice,
    ProductStatus status,
    Long discount,
    DiscountType discountType,
    BrandInfo brand,
    Long likeCount
) {
    public static ProductInfo from(Product product, BrandInfo brandInfo, Long likeCount) {
        return new ProductInfo(
            product.getId(),
            product.getName(),
            product.getProductCode(),
            product.getBasePrice(),
            product.calculateDiscountedPrice(),
            product.getStatus(),
            product.getDiscount(),
            product.getDiscountType(),
            brandInfo,
            likeCount
        );
    }
}