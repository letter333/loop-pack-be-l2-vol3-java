package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MemberCouponRepository {

    Optional<MemberCoupon> findById(Long id);

    Optional<MemberCoupon> findByIdWithCoupon(Long id);

    Optional<MemberCoupon> findByMemberIdAndCouponId(Long memberId, Long couponId);

    Optional<MemberCoupon> findByUsedOrderId(Long orderId);

    List<MemberCoupon> findAllByMemberId(Long memberId);

    List<MemberCoupon> findAllByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    List<Long> findIssuedCouponIdsByMemberId(Long memberId);

    MemberCoupon save(MemberCoupon memberCoupon);

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

    Page<MemberCoupon> findAllByCouponId(Long couponId, Pageable pageable);
}
