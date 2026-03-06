package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Optional<Coupon> findById(Long id);

    Optional<Coupon> findByIdForUpdate(Long id);

    List<IssuableCoupon> findAllIssuableWithIssuedFlag(Long memberId);

    Page<Coupon> findAllActive(Pageable pageable);

    boolean existsActiveById(Long id);

    Coupon save(Coupon coupon);

    void delete(Long id);
}
