package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public final class ProductValidator {

    private ProductValidator() {
        // 인스턴스화 방지
    }

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

    public static void validateDiscount(Long discount, DiscountType discountType, Long basePrice) {
        // 둘 다 null이면 할인 없음 - 유효
        if (discount == null && discountType == null) {
            return;
        }

        // 둘 중 하나만 null이면 오류
        if (discount == null || discountType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액과 할인 타입은 함께 설정되어야 합니다.");
        }

        // 음수 할인 검증
        if (discount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.");
        }

        if (discountType == DiscountType.RATE && discount > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인은 100%를 초과할 수 없습니다.");
        }

        if (discountType == DiscountType.PRICE && discount > basePrice) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정액 할인은 기본 가격을 초과할 수 없습니다.");
        }
    }

    public static void validateStatus(ProductStatus status) {
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 상태는 필수입니다.");
        }
    }
}