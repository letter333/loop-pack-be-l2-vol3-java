package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.MemberCouponIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class MemberCouponIssueRepositoryImpl implements MemberCouponIssueRepository {

    private final MemberCouponIssueJpaRepository memberCouponIssueJpaRepository;

    @Override
    public void save(Long memberId, Long couponId, String couponCode, String status, LocalDateTime expiredAt) {
        MemberCouponIssueEntity entity = MemberCouponIssueEntity.of(memberId, couponId, couponCode, status, expiredAt);
        memberCouponIssueJpaRepository.save(entity);
    }
}
