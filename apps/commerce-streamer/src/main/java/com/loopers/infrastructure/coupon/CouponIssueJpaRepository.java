package com.loopers.infrastructure.coupon;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponIssueJpaRepository extends JpaRepository<CouponIssueEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CouponIssueEntity c WHERE c.id = :id")
    Optional<CouponIssueEntity> findByIdForUpdate(@Param("id") Long id);
}
