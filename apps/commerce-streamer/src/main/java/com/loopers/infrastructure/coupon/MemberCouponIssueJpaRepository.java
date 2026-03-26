package com.loopers.infrastructure.coupon;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberCouponIssueJpaRepository extends JpaRepository<MemberCouponIssueEntity, Long> {
}
