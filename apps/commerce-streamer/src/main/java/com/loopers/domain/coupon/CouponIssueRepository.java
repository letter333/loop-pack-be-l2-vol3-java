package com.loopers.domain.coupon;

import java.util.Optional;

public interface CouponIssueRepository {

    Optional<CouponIssueDomain> findByIdForUpdate(Long couponId);

    void save(CouponIssueDomain coupon);
}
