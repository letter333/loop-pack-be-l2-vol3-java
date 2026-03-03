package com.loopers.infrastructure.coupon;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@SQLDelete(sql = "UPDATE coupons SET deleted_at = NOW() WHERE id = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "coupon_type", nullable = false, length = 20)
    private CouponType couponType;

    @Column(name = "discount_value", nullable = false)
    private Long discountValue;

    @Column(name = "min_order_amount")
    private Long minOrderAmount;

    @Column(name = "max_discount_amount")
    private Long maxDiscountAmount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    public static CouponEntity from(Coupon coupon) {
        CouponEntity entity = new CouponEntity();
        entity.name = coupon.getName();
        entity.description = coupon.getDescription();
        entity.couponType = coupon.getCouponType();
        entity.discountValue = coupon.getDiscountValue();
        entity.minOrderAmount = coupon.getMinOrderAmount();
        entity.maxDiscountAmount = coupon.getMaxDiscountAmount();
        entity.totalQuantity = coupon.getTotalQuantity();
        entity.issuedQuantity = coupon.getIssuedQuantity() != null ? coupon.getIssuedQuantity() : 0;
        entity.validFrom = coupon.getValidFrom();
        entity.validUntil = coupon.getValidUntil();
        return entity;
    }

    public Coupon toDomain() {
        return new Coupon(
            getId(),
            name,
            description,
            couponType,
            discountValue,
            minOrderAmount,
            maxDiscountAmount,
            totalQuantity,
            issuedQuantity,
            validFrom,
            validUntil,
            getCreatedAt() != null ? getCreatedAt().toLocalDateTime() : null,
            getUpdatedAt() != null ? getUpdatedAt().toLocalDateTime() : null,
            getDeletedAt() != null ? getDeletedAt().toLocalDateTime() : null
        );
    }

    public void update(String name, String description, CouponType couponType,
                       Long discountValue, Long minOrderAmount, Long maxDiscountAmount,
                       Integer totalQuantity, Integer issuedQuantity,
                       LocalDateTime validFrom, LocalDateTime validUntil) {
        this.name = name;
        this.description = description;
        this.couponType = couponType;
        this.discountValue = discountValue;
        this.minOrderAmount = minOrderAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }
}
