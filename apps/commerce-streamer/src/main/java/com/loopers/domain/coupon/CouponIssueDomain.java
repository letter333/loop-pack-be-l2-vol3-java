package com.loopers.domain.coupon;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CouponIssueDomain {

    private Long id;
    private Integer totalQuantity;
    private Integer issuedQuantity;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private LocalDateTime deletedAt;

    public CouponIssueDomain(Long id, Integer totalQuantity, Integer issuedQuantity,
                             LocalDateTime validFrom, LocalDateTime validUntil, LocalDateTime deletedAt) {
        this.id = id;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
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

    public boolean canIssue() {
        return !isDeleted() && isWithinIssuePeriod() && hasRemainingQuantity();
    }

    public void issue() {
        this.issuedQuantity++;
    }
}
