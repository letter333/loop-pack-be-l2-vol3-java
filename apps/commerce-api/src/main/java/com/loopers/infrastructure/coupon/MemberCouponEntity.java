package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(
    name = "member_coupons",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_coupons_code", columnNames = {"coupon_code"})
    },
    indexes = {
        @Index(name = "idx_member_coupons_member", columnList = "member_id"),
        @Index(name = "idx_member_coupons_member_coupon", columnList = "member_id, coupon_id"),
        @Index(name = "idx_member_coupons_order", columnList = "used_order_id")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCouponEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "coupon_code", nullable = false, unique = true, length = 20)
    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MemberCouponStatus status;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", insertable = false, updatable = false)
    private CouponEntity coupon;

    @PrePersist
    private void prePersist() {
        ZonedDateTime now = ZonedDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    public static MemberCouponEntity from(MemberCoupon memberCoupon) {
        MemberCouponEntity entity = new MemberCouponEntity();
        entity.memberId = memberCoupon.getMemberId();
        entity.couponId = memberCoupon.getCouponId();
        entity.couponCode = memberCoupon.getCouponCode();
        entity.status = memberCoupon.getStatus();
        entity.usedOrderId = memberCoupon.getUsedOrderId();
        entity.usedAt = memberCoupon.getUsedAt();
        entity.issuedAt = memberCoupon.getIssuedAt();
        entity.expiredAt = memberCoupon.getExpiredAt();
        return entity;
    }

    public MemberCoupon toDomain() {
        return new MemberCoupon(
            id,
            memberId,
            couponId,
            couponCode,
            status,
            usedOrderId,
            usedAt,
            issuedAt,
            expiredAt,
            createdAt != null ? createdAt.toLocalDateTime() : null,
            updatedAt != null ? updatedAt.toLocalDateTime() : null
        );
    }

    public MemberCoupon toDomainWithCoupon() {
        MemberCoupon memberCoupon = toDomain();
        if (coupon != null) {
            memberCoupon.setCoupon(coupon.toDomain());
        }
        return memberCoupon;
    }

    public void update(MemberCouponStatus status, Long usedOrderId, LocalDateTime usedAt) {
        this.status = status;
        this.usedOrderId = usedOrderId;
        this.usedAt = usedAt;
    }
}
