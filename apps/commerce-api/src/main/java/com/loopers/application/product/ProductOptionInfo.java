package com.loopers.application.product;

import com.loopers.domain.product.ProductOption;

public record ProductOptionInfo(
    Long id,
    String optionValue,
    String displayName,
    Long extraPrice,
    Integer stockQuantity
) {
    public static ProductOptionInfo from(ProductOption option) {
        return new ProductOptionInfo(
            option.getId(),
            option.getOptionValue(),
            option.getDisplayName(),
            option.getExtraPrice(),
            option.getStockQuantity()
        );
    }
}
