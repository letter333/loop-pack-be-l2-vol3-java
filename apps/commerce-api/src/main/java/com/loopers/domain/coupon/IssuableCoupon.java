package com.loopers.domain.coupon;

public record IssuableCoupon(
    Coupon coupon,
    boolean issued
) {
}
