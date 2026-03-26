package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponIssueDomain;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CouponIssueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "issued_quantity", nullable = false)
    private Integer issuedQuantity;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public CouponIssueDomain toDomain() {
        return new CouponIssueDomain(id, totalQuantity, issuedQuantity, validFrom, validUntil, deletedAt);
    }

    public void incrementIssuedQuantity() {
        this.issuedQuantity++;
    }
}
