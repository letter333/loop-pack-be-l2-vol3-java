package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public class ProductValidator {

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명은 필수입니다.");
        }
    }

    public static void validateBrandId(Long brandId) {
        if (brandId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 필수입니다.");
        }
    }

    public static void validateCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리 ID는 필수입니다.");
        }
    }

    public static void validateBasePrice(Long basePrice) {
        if (basePrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "기본 가격은 필수입니다.");
        }
        if (basePrice < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "기본 가격은 0 이상이어야 합니다.");
        }
    }
}