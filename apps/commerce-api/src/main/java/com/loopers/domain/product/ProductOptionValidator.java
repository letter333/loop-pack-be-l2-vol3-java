package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class ProductOptionValidator {

    public static void validateOptionValue(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "옵션값은 필수입니다.");
        }
    }

    public static void validateStockQuantity(Integer stockQuantity) {
        if (stockQuantity == null || stockQuantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
    }
}