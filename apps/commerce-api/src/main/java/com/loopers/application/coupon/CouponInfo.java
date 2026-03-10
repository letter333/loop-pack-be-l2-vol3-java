package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;

import java.time.LocalDateTime;

public record CouponInfo(
    Long id,
    String name,
    String description,
    CouponType couponType,
    Long discountValue,
    Long minOrderAmount,
    Long maxDiscountAmount,
    Integer totalQuantity,
    Integer issuedQuantity,
    Integer remainingQuantity,
    LocalDateTime validFrom,
    LocalDateTime validUntil,
    boolean isIssued
) {
    public static CouponInfo from(Coupon coupon, boolean isIssued) {
        return new CouponInfo(
            coupon.getId(),
            coupon.getName(),
            coupon.getDescription(),
            coupon.getCouponType(),
            coupon.getDiscountValue(),
            coupon.getMinOrderAmount(),
            coupon.getMaxDiscountAmount(),
            coupon.getTotalQuantity(),
            coupon.getIssuedQuantity(),
            coupon.getRemainingQuantity(),
            coupon.getValidFrom(),
            coupon.getValidUntil(),
            isIssued
        );
    }
}
