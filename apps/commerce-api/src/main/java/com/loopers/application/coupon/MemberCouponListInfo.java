package com.loopers.application.coupon;

import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponStatus;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

public record MemberCouponListInfo(
    Page<MemberCouponInfo> coupons,
    long availableCount,
    long usedCount,
    long expiredCount
) {
    public static MemberCouponListInfo of(
        Page<MemberCoupon> memberCoupons,
        long availableCount,
        long usedCount,
        long expiredCount
    ) {
        Page<MemberCouponInfo> coupons = memberCoupons.map(MemberCouponInfo::from);
        return new MemberCouponListInfo(coupons, availableCount, usedCount, expiredCount);
    }
}
