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

    Optional<MemberCouponEntity> findByMemberIdAndCouponId(Long memberId, Long couponId);

    Optional<MemberCouponEntity> findByUsedOrderId(Long orderId);

    List<MemberCouponEntity> findAllByMemberId(Long memberId);

    List<MemberCouponEntity> findAllByMemberIdAndStatus(Long memberId, MemberCouponStatus status);

    @Query("SELECT mc.couponId FROM MemberCouponEntity mc WHERE mc.memberId = :memberId")
    List<Long> findCouponIdsByMemberId(@Param("memberId") Long memberId);

    boolean existsByMemberIdAndCouponId(Long memberId, Long couponId);

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

    @Query("SELECT COUNT(mc) FROM MemberCouponEntity mc " +
           "WHERE mc.memberId = :memberId AND mc.status = 'AVAILABLE' AND mc.expiredAt > CURRENT_TIMESTAMP")
    long countAvailableByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT COUNT(mc) FROM MemberCouponEntity mc " +
           "WHERE mc.memberId = :memberId AND mc.status = :status")
    long countByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") MemberCouponStatus status);

    @Query("SELECT COUNT(mc) FROM MemberCouponEntity mc " +
           "WHERE mc.memberId = :memberId AND (mc.status = 'EXPIRED' OR (mc.status = 'AVAILABLE' AND mc.expiredAt <= CURRENT_TIMESTAMP))")
    long countExpiredByMemberId(@Param("memberId") Long memberId);

    Page<MemberCouponEntity> findAllByCouponId(Long couponId, Pageable pageable);
}
