package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

import java.util.List;

public record ProductDetailInfo(
    Long id,
    String name,
    String productCode,
    Long basePrice,
    Long discountedPrice,
    ProductStatus status,
    Long discount,
    DiscountType discountType,
    BrandInfo brand,
    Long likeCount,
    List<ProductOptionInfo> options,
    List<ProductImageInfo> images
) {
    public static ProductDetailInfo from(Product product, BrandInfo brand, Long likeCount) {
        return new ProductDetailInfo(
            product.getId(),
            product.getName(),
            product.getProductCode(),
            product.getBasePrice(),
            product.calculateDiscountedPrice(),
            product.getStatus(),
            product.getDiscount(),
            product.getDiscountType(),
            brand,
            likeCount,
            product.getOptions().stream().map(ProductOptionInfo::from).toList(),
            product.getImages().stream().map(ProductImageInfo::from).toList()
        );
    }
}