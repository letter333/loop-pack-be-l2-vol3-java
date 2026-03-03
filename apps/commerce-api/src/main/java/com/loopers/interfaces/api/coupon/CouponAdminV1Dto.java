package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponDetailInfo;
import com.loopers.domain.coupon.CouponType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CouponAdminV1Dto {

    public record CreateCouponRequest(
        @NotBlank(message = "쿠폰명은 비어있을 수 없습니다.")
        @Size(max = 100, message = "쿠폰명은 100자를 초과할 수 없습니다.")
        String name,

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description,

        @NotNull(message = "할인 타입은 필수입니다.")
        CouponType couponType,

        @NotNull(message = "할인값은 필수입니다.")
        @Positive(message = "할인값은 0보다 커야 합니다.")
        Long discountValue,

        @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
        Long minOrderAmount,

        @PositiveOrZero(message = "최대 할인 금액은 0 이상이어야 합니다.")
        Long maxDiscountAmount,

        @NotNull(message = "총 발급 가능 수량은 필수입니다.")
        @Positive(message = "총 발급 가능 수량은 0보다 커야 합니다.")
        Integer totalQuantity,

        @NotNull(message = "발급 가능 시작일은 필수입니다.")
        LocalDateTime validFrom,

        @NotNull(message = "쿠폰 만료일은 필수입니다.")
        LocalDateTime validUntil
    ) {}

    public record UpdateCouponRequest(
        @NotBlank(message = "쿠폰명은 비어있을 수 없습니다.")
        @Size(max = 100, message = "쿠폰명은 100자를 초과할 수 없습니다.")
        String name,

        @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
        String description,

        @NotNull(message = "할인 타입은 필수입니다.")
        CouponType couponType,

        @NotNull(message = "할인값은 필수입니다.")
        @Positive(message = "할인값은 0보다 커야 합니다.")
        Long discountValue,

        @PositiveOrZero(message = "최소 주문 금액은 0 이상이어야 합니다.")
        Long minOrderAmount,

        @PositiveOrZero(message = "최대 할인 금액은 0 이상이어야 합니다.")
        Long maxDiscountAmount,

        @NotNull(message = "총 발급 가능 수량은 필수입니다.")
        @Positive(message = "총 발급 가능 수량은 0보다 커야 합니다.")
        Integer totalQuantity,

        @NotNull(message = "발급 가능 시작일은 필수입니다.")
        LocalDateTime validFrom,

        @NotNull(message = "쿠폰 만료일은 필수입니다.")
        LocalDateTime validUntil
    ) {}

    public record CouponDetailResponse(
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
        public static CouponDetailResponse from(CouponDetailInfo info) {
            return new CouponDetailResponse(
                info.id(),
                info.name(),
                info.description(),
                info.couponType(),
                info.discountValue(),
                info.minOrderAmount(),
                info.maxDiscountAmount(),
                info.totalQuantity(),
                info.issuedQuantity(),
                info.remainingQuantity(),
                info.validFrom(),
                info.validUntil(),
                info.createdAt(),
                info.updatedAt()
            );
        }
    }
}
