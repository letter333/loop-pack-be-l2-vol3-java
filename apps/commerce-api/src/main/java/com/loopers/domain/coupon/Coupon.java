package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Coupon {

    private Long id;
    private String name;
    private String description;
    private CouponType couponType;
    private Long discountValue;
    private Long minOrderAmount;
    private Long maxDiscountAmount;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Coupon(String name, String description, CouponType couponType,
                  Long discountValue, Long minOrderAmount, Long maxDiscountAmount,
                  Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        CouponValidator.validateName(name);
        CouponValidator.validateCouponType(couponType);
        CouponValidator.validateDiscountValue(discountValue, couponType);
        CouponValidator.validateMinOrderAmount(minOrderAmount);
        CouponValidator.validateTotalQuantity(totalQuantity);
        CouponValidator.validateValidPeriod(validFrom, validUntil);

        this.name = name;
        this.description = description;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : 0L;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = 0;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public Coupon(Long id, String name, String description, CouponType couponType,
                  Long discountValue, Long minOrderAmount, Long maxDiscountAmount,
                  Integer totalQuantity, Integer issuedQuantity,
                  LocalDateTime validFrom, LocalDateTime validUntil,
                  LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : 0L;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity != null ? issuedQuantity : 0;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isWithinIssuePeriod() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    public boolean hasRemainingQuantity() {
        return issuedQuantity < totalQuantity;
    }

    public int getRemainingQuantity() {
        return totalQuantity - issuedQuantity;
    }

    public boolean canIssue() {
        return !isDeleted() && isWithinIssuePeriod() && hasRemainingQuantity();
    }

    public void issue() {
        if (!canIssue()) {
            if (isDeleted()) {
                throw new CoreException(ErrorType.NOT_FOUND, "삭제된 쿠폰입니다.");
            }
            if (!isWithinIssuePeriod()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급 기간이 아닙니다.");
            }
            if (!hasRemainingQuantity()) {
                throw new CoreException(ErrorType.BAD_REQUEST, "쿠폰 발급 수량이 모두 소진되었습니다.");
            }
        }
        this.issuedQuantity++;
    }

    public Long calculateDiscount(Long orderAmount) {
        if (orderAmount < minOrderAmount) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("최소 주문 금액 %d원 이상이어야 합니다.", minOrderAmount));
        }

        long discount = switch (couponType) {
            case FIXED -> discountValue;
            case RATE -> orderAmount * discountValue / 100;
        };

        if (couponType == CouponType.RATE && maxDiscountAmount != null) {
            discount = Math.min(discount, maxDiscountAmount);
        }

        return Math.min(discount, orderAmount);
    }

    public void update(String name, String description, CouponType couponType,
                       Long discountValue, Long minOrderAmount, Long maxDiscountAmount,
                       Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        CouponValidator.validateName(name);
        CouponValidator.validateCouponType(couponType);
        CouponValidator.validateDiscountValue(discountValue, couponType);
        CouponValidator.validateMinOrderAmount(minOrderAmount);
        CouponValidator.validateTotalQuantity(totalQuantity);
        CouponValidator.validateValidPeriod(validFrom, validUntil);

        if (totalQuantity < this.issuedQuantity) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("총 발급 가능 수량은 이미 발급된 수량(%d)보다 작을 수 없습니다.", this.issuedQuantity));
        }

        this.name = name;
        this.description = description;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount != null ? minOrderAmount : 0L;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }
}
