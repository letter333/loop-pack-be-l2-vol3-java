package com.loopers.domain.coupon;

public interface MemberCouponIssueRepository {

    void save(Long memberId, Long couponId, String couponCode, String status, java.time.LocalDateTime expiredAt);
}
