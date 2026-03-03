package com.loopers.infrastructure.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CouponJpaRepository extends JpaRepository<CouponEntity, Long> {

    Optional<CouponEntity> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT c FROM CouponEntity c " +
           "WHERE c.deletedAt IS NULL " +
           "AND c.validFrom <= :now " +
           "AND c.validUntil >= :now " +
           "AND c.issuedQuantity < c.totalQuantity")
    List<CouponEntity> findAllIssuable(LocalDateTime now);

    Page<CouponEntity> findAllByDeletedAtIsNull(Pageable pageable);
}
