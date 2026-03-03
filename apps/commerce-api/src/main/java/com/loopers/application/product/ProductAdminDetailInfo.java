package com.loopers.application.product;

import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProductAdminDetailInfo(
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
    List<ProductOptionInfo> options,
    List<ProductImageInfo> images,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public static ProductAdminDetailInfo from(Product product) {
        return new ProductAdminDetailInfo(
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
            product.getOptions().stream().map(ProductOptionInfo::from).toList(),
            product.getImages().stream().map(ProductImageInfo::from).toList(),
            product.getCreatedAt(),
            product.getUpdatedAt(),
            product.getDeletedAt()
        );
    }
}
