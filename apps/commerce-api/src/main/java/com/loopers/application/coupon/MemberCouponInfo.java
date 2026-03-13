package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponStatus;

import java.time.LocalDateTime;

public record MemberCouponInfo(
    Long id,
    Long couponId,
    String couponCode,
    String couponName,
    String description,
    CouponType couponType,
    Long discountValue,
    Long minOrderAmount,
    Long maxDiscountAmount,
    MemberCouponStatus status,
    LocalDateTime issuedAt,
    LocalDateTime expiredAt,
    LocalDateTime usedAt,
    boolean isAvailable
) {
    public static MemberCouponInfo from(MemberCoupon memberCoupon) {
        var coupon = memberCoupon.getCoupon();
        return new MemberCouponInfo(
            memberCoupon.getId(),
            memberCoupon.getCouponId(),
            memberCoupon.getCouponCode(),
            coupon != null ? coupon.getName() : null,
            coupon != null ? coupon.getDescription() : null,
            coupon != null ? coupon.getCouponType() : null,
            coupon != null ? coupon.getDiscountValue() : null,
            coupon != null ? coupon.getMinOrderAmount() : null,
            coupon != null ? coupon.getMaxDiscountAmount() : null,
            memberCoupon.getStatus(),
            memberCoupon.getIssuedAt(),
            memberCoupon.getExpiredAt(),
            memberCoupon.getUsedAt(),
            memberCoupon.isAvailable()
        );
    }
}
