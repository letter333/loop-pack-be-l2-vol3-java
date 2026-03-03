package com.loopers.domain.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {

    Optional<Coupon> findById(Long id);

    List<Coupon> findAllIssuable();

    Page<Coupon> findAllActive(Pageable pageable);

    Coupon save(Coupon coupon);

    void delete(Long id);
}
