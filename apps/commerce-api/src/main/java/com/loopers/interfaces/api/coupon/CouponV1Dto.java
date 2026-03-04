package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.MemberCouponInfo;
import com.loopers.application.coupon.MemberCouponListInfo;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.MemberCouponStatus;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public class CouponV1Dto {

    public record CouponResponse(
        Long id,
        String name,
        String description,
        CouponType couponType,
        Long discountValue,
        Long minOrderAmount,
        Long maxDiscountAmount,
        Integer remainingQuantity,
        LocalDateTime validFrom,
        LocalDateTime validUntil,
        boolean isIssued
    ) {
        public static CouponResponse from(CouponInfo info) {
            return new CouponResponse(
                info.id(),
                info.name(),
                info.description(),
                info.couponType(),
                info.discountValue(),
                info.minOrderAmount(),
                info.maxDiscountAmount(),
                info.remainingQuantity(),
                info.validFrom(),
                info.validUntil(),
                info.isIssued()
            );
        }
    }

    public record MemberCouponResponse(
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
        public static MemberCouponResponse from(MemberCouponInfo info) {
            return new MemberCouponResponse(
                info.id(),
                info.couponId(),
                info.couponCode(),
                info.couponName(),
                info.description(),
                info.couponType(),
                info.discountValue(),
                info.minOrderAmount(),
                info.maxDiscountAmount(),
                info.status(),
                info.issuedAt(),
                info.expiredAt(),
                info.usedAt(),
                info.isAvailable()
            );
        }
    }

    public record MemberCouponListResponse(
        Page<MemberCouponResponse> coupons,
        long availableCount,
        long usedCount,
        long expiredCount
    ) {
        public static MemberCouponListResponse from(MemberCouponListInfo info) {
            return new MemberCouponListResponse(
                info.coupons().map(MemberCouponResponse::from),
                info.availableCount(),
                info.usedCount(),
                info.expiredCount()
            );
        }
    }
}
