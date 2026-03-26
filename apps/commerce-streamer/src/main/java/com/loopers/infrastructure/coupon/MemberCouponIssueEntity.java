package com.loopers.infrastructure.coupon;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Table(name = "member_coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberCouponIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "coupon_id", nullable = false)
    private Long couponId;

    @Column(name = "coupon_code", nullable = false, unique = true, length = 20)
    private String couponCode;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "used_order_id")
    private Long usedOrderId;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

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

    public static MemberCouponIssueEntity of(Long memberId, Long couponId, String couponCode,
                                              String status, LocalDateTime expiredAt) {
        MemberCouponIssueEntity entity = new MemberCouponIssueEntity();
        entity.memberId = memberId;
        entity.couponId = couponId;
        entity.couponCode = couponCode;
        entity.status = status;
        entity.issuedAt = LocalDateTime.now();
        entity.expiredAt = expiredAt;
        return entity;
    }
}
