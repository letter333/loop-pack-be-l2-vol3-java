package com.loopers.application.product;

import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.ProductStatus;

public class ProductCommand {

    public record Create(
        String name,
        Long brandId,
        Long categoryId,
        Long basePrice
    ) {}

    public record Update(
        String name,
        Long categoryId,
        Long basePrice,
        Long discount,
        DiscountType discountType,
        ProductStatus status
    ) {}
}