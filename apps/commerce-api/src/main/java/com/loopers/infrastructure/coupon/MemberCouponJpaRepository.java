package com.loopers.infrastructure.coupon;

import com.loopers.domain.coupon.MemberCouponStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MemberCouponJpaRepository extends JpaRepository<MemberCouponEntity, Long> {

    @Query(value = "SELECT id FROM member_coupons WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);

    Optional<MemberCouponEntity> findByMemberIdAndCouponId(Long memberId, Long couponId);

    Optional<MemberCouponEntity> findByUsedOrderId(Long orderId);

    List<MemberCouponEntity> findAllByMemberId(Long memberId);

    List<MemberCouponEntity> findAllByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    @Query("SELECT mc FROM MemberCouponEntity mc " +
           "JOIN FETCH mc.coupon c " +
           "WHERE mc.id = :id")
    Optional<MemberCouponEntity> findByIdWithCoupon(@Param("id") Long id);

    @Query("SELECT mc FROM MemberCouponEntity mc " +
           "LEFT JOIN FETCH mc.coupon " +
           "WHERE mc.memberId = :memberId")
    List<MemberCouponEntity> findAllByMemberIdWithCoupon(@Param("memberId") Long memberId);

    @Query("SELECT mc FROM MemberCouponEntity mc " +
           "LEFT JOIN FETCH mc.coupon " +
           "WHERE mc.memberId = :memberId AND mc.status = :status")
    List<MemberCouponEntity> findAllByMemberIdAndStatusWithCoupon(
        @Param("memberId") Long memberId,
        @Param("status") MemberCouponStatus status
    );

    @Query(value = "SELECT mc FROM MemberCouponEntity mc " +
           "LEFT JOIN FETCH mc.coupon " +
           "WHERE mc.memberId = :memberId",
           countQuery = "SELECT COUNT(mc) FROM MemberCouponEntity mc WHERE mc.memberId = :memberId")
    Page<MemberCouponEntity> findAllByMemberIdWithCoupon(@Param("memberId") Long memberId, Pageable pageable);

    @Query(value = "SELECT mc FROM MemberCouponEntity mc " +
           "LEFT JOIN FETCH mc.coupon " +
           "WHERE mc.memberId = :memberId AND mc.status = :status",
           countQuery = "SELECT COUNT(mc) FROM MemberCouponEntity mc WHERE mc.memberId = :memberId AND mc.status = :status")
    Page<MemberCouponEntity> findAllByMemberIdAndStatusWithCoupon(
        @Param("memberId") Long memberId,
        @Param("status") MemberCouponStatus status,
        Pageable pageable
    );

    Page<MemberCouponEntity> findAllByCouponId(Long couponId, Pageable pageable);

    @Query("SELECT " +
           "SUM(CASE WHEN mc.status = 'AVAILABLE' AND mc.expiredAt > CURRENT_TIMESTAMP THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN mc.status = 'USED' THEN 1 ELSE 0 END), " +
           "SUM(CASE WHEN mc.status = 'EXPIRED' OR (mc.status = 'AVAILABLE' AND mc.expiredAt <= CURRENT_TIMESTAMP) THEN 1 ELSE 0 END) " +
           "FROM MemberCouponEntity mc WHERE mc.memberId = :memberId")
    List<Object[]> countStatusesByMemberId(@Param("memberId") Long memberId);
}
