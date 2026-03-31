package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.CouponIssueDomain;
import com.loopers.domain.coupon.CouponIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CouponIssueRepositoryImpl implements CouponIssueRepository {

    private final CouponIssueJpaRepository couponIssueJpaRepository;

    @Override
    public Optional<CouponIssueDomain> findByIdForUpdate(Long couponId) {
        return couponIssueJpaRepository.findByIdForUpdate(couponId)
            .map(CouponIssueEntity::toDomain);
    }

    @Override
    public void save(CouponIssueDomain coupon) {
        CouponIssueEntity entity = couponIssueJpaRepository.findById(coupon.getId())
            .orElseThrow(() -> new IllegalStateException("쿠폰을 찾을 수 없습니다: " + coupon.getId()));
        entity.incrementIssuedQuantity();
    }
}
