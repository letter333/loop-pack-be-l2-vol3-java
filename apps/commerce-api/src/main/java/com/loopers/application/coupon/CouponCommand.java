package com.loopers.application.coupon;

import com.loopers.domain.coupon.CouponType;

import java.time.LocalDateTime;

public class CouponCommand {

    public record Create(
        String name,
        String description,
        CouponType couponType,
        Long discountValue,
        Long minOrderAmount,
        Long maxDiscountAmount,
        Integer totalQuantity,
        LocalDateTime validFrom,
        LocalDateTime validUntil
    ) {}

    public record Update(
        String name,
        String description,
        CouponType couponType,
        Long discountValue,
        Long minOrderAmount,
        Long maxDiscountAmount,
        Integer totalQuantity,
        LocalDateTime validFrom,
        LocalDateTime validUntil
    ) {}
}
