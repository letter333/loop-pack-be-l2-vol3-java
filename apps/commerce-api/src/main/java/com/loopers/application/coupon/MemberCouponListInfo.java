package com.loopers.application.coupon;

import com.loopers.domain.coupon.MemberCoupon;

import java.util.List;

public record MemberCouponListInfo(
    List<MemberCouponInfo> coupons,
    int totalCount,
    int availableCount,
    int usedCount,
    int expiredCount
) {
    public static MemberCouponListInfo from(List<MemberCoupon> memberCoupons) {
        List<MemberCouponInfo> coupons = memberCoupons.stream()
            .map(MemberCouponInfo::from)
            .toList();

        int availableCount = (int) coupons.stream()
            .filter(MemberCouponInfo::isAvailable)
            .count();

        int usedCount = (int) memberCoupons.stream()
            .filter(mc -> mc.getStatus() == com.loopers.domain.coupon.MemberCouponStatus.USED)
            .count();

        int expiredCount = (int) memberCoupons.stream()
            .filter(mc -> mc.getStatus() == com.loopers.domain.coupon.MemberCouponStatus.EXPIRED || mc.isExpired())
            .count();

        return new MemberCouponListInfo(
            coupons,
            coupons.size(),
            availableCount,
            usedCount,
            expiredCount
        );
    }
}
