package com.loopers.infrastructure.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    Optional<CouponEntity> findByIdAndDeletedAtIsNull(Long id);

    Page<CouponEntity> findAllByDeletedAtIsNull(Pageable pageable);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    @Query(value = "SELECT id FROM coupons WHERE id = :id FOR UPDATE", nativeQuery = true)
    Optional<Long> lockById(@Param("id") Long id);

    @Query("SELECT c, " +
           "CASE WHEN mc.id IS NOT NULL THEN true ELSE false END " +
           "FROM CouponEntity c " +
           "LEFT JOIN MemberCouponEntity mc ON mc.couponId = c.id AND mc.memberId = :memberId " +
           "WHERE c.deletedAt IS NULL " +
           "AND c.validFrom <= :now " +
           "AND c.validUntil >= :now " +
           "AND c.issuedQuantity < c.totalQuantity")
    List<Object[]> findAllIssuableWithIssuedFlag(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);
}
