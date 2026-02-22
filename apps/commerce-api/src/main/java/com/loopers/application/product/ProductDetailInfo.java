package com.loopers.application.product;

import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

import java.time.LocalDateTime;

public record ProductDetailInfo(
    Long id,
    String name,
    String productCode,
    Long brandId,
    Long categoryId,
    Long basePrice,
    Long discountedPrice,
    ProductStatus status,
    Long discount,
    DiscountType discountType,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public static ProductDetailInfo from(Product product) {
        return new ProductDetailInfo(
            product.getId(),
            product.getName(),
            product.getProductCode(),
            product.getBrandId(),
            product.getCategoryId(),
            product.getBasePrice(),
            product.calculateDiscountedPrice(),
            product.getStatus(),
            product.getDiscount(),
            product.getDiscountType(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getDeletedAt()
        );
    }
}