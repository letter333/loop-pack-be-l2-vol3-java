package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import java.time.LocalDateTime;

public final class CouponValidator {

    private CouponValidator() {
        // 인스턴스화 방지
    }

    public static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰명은 필수입니다.");
        }
    }

    public static void validateCouponType(CouponType couponType) {
        if (couponType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 타입은 필수입니다.");
        }
    }

    public static void validateDiscountValue(Long discountValue, CouponType couponType) {
        if (discountValue == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인값은 필수입니다.");
        }
        if (discountValue <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인값은 0보다 커야 합니다.");
        }
        if (couponType == CouponType.PERCENTAGE && (discountValue < 1 || discountValue > 100)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "정률 할인은 1~100 사이여야 합니다.");
        }
    }

    public static void validateMinOrderAmount(Long minOrderAmount) {
        if (minOrderAmount != null && minOrderAmount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액은 0 이상이어야 합니다.");
        }
    }

    public static void validateTotalQuantity(Integer totalQuantity) {
        if (totalQuantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 발급 가능 수량은 필수입니다.");
        }
        if (totalQuantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 발급 가능 수량은 0보다 커야 합니다.");
        }
    }

    public static void validateValidPeriod(LocalDateTime validFrom, LocalDateTime validUntil) {
        if (validFrom == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 가능 시작일은 필수입니다.");
        }
        if (validUntil == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 만료일은 필수입니다.");
        }
        if (!validFrom.isBefore(validUntil)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "발급 가능 시작일은 만료일 이전이어야 합니다.");
        }
    }
}
