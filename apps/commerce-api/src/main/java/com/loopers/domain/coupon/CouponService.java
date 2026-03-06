package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Coupon getCoupon(Long couponId) {
        return couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Coupon getActiveCoupon(Long couponId) {
        Coupon coupon = getCoupon(couponId);
        if (coupon.isDeleted()) {
            throw new CoreException(ErrorType.NOT_FOUND, "삭제된 쿠폰입니다.");
        }
        return coupon;
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public void validateCouponExists(Long couponId) {
        if (!couponRepository.existsActiveById(couponId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다.");
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<IssuableCoupon> getIssuableCouponsWithIssuedFlag(Long memberId) {
        return couponRepository.findAllIssuableWithIssuedFlag(memberId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Coupon> getCouponsForAdmin(Pageable pageable) {
        return couponRepository.findAllActive(pageable);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Coupon createCoupon(String name, String description, CouponType couponType,
                                Long discountValue, Long minOrderAmount, Long maxDiscountAmount,
                                Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        Coupon coupon = new Coupon(name, description, couponType, discountValue,
            minOrderAmount, maxDiscountAmount, totalQuantity, validFrom, validUntil);
        return couponRepository.save(coupon);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Coupon updateCoupon(Long couponId, String name, String description, CouponType couponType,
                                Long discountValue, Long minOrderAmount, Long maxDiscountAmount,
                                Integer totalQuantity, LocalDateTime validFrom, LocalDateTime validUntil) {
        Coupon coupon = getActiveCoupon(couponId);
        coupon.update(name, description, couponType, discountValue,
            minOrderAmount, maxDiscountAmount, totalQuantity, validFrom, validUntil);
        return couponRepository.save(coupon);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteCoupon(Long couponId) {
        Coupon coupon = getActiveCoupon(couponId);
        coupon.delete();
        couponRepository.save(coupon);
    }

}
