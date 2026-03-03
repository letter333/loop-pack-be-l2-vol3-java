package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;

import java.time.LocalDateTime;

public record CouponDetailInfo(
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
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CouponDetailInfo from(Coupon coupon) {
        return new CouponDetailInfo(
            coupon.getId(),
            coupon.getName(),
            coupon.getDescription(),
            coupon.getCouponType(),
            coupon.getDiscountValue(),
            coupon.getMinOrderAmount(),
            coupon.getMaxDiscountAmount(),
            coupon.getTotalQuantity(),
            coupon.getIssuedQuantity(),
            coupon.getTotalQuantity() - coupon.getIssuedQuantity(),
            coupon.getValidFrom(),
            coupon.getValidUntil(),
            coupon.getCreatedAt(),
            coupon.getUpdatedAt()
        );
    }
}
