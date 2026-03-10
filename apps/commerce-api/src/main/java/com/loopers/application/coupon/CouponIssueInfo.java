package com.loopers.application.coupon;

import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponStatus;

import java.time.LocalDateTime;

public record CouponIssueInfo(
    Long id,
    Long memberId,
    String couponCode,
    MemberCouponStatus status,
    LocalDateTime issuedAt,
    LocalDateTime usedAt,
    Long usedOrderId
) {
    public static CouponIssueInfo from(MemberCoupon memberCoupon) {
        return new CouponIssueInfo(
            memberCoupon.getId(),
            memberCoupon.getMemberId(),
            memberCoupon.getCouponCode(),
            memberCoupon.getStatus(),
            memberCoupon.getIssuedAt(),
            memberCoupon.getUsedAt(),
            memberCoupon.getUsedOrderId()
        );
    }
}
