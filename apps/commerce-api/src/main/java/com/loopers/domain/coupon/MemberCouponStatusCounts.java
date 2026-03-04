package com.loopers.domain.coupon;

public record MemberCouponStatusCounts(
    long availableCount,
    long usedCount,
    long expiredCount
) {
}
