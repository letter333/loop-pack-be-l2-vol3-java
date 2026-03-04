package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;
    private final CouponRepository couponRepository;
    private final CouponCodeGenerator couponCodeGenerator;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public MemberCoupon getMemberCoupon(Long memberCouponId) {
        return memberCouponRepository.findById(memberCouponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public MemberCoupon getMemberCouponWithCoupon(Long memberCouponId) {
        return memberCouponRepository.findByIdWithCoupon(memberCouponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<MemberCoupon> getMemberCoupons(Long memberId, Pageable pageable) {
        return memberCouponRepository.findAllByMemberId(memberId, pageable);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<MemberCoupon> getMemberCouponsByStatus(Long memberId, MemberCouponStatus status, Pageable pageable) {
        return memberCouponRepository.findAllByMemberIdAndStatus(memberId, status, pageable);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public long countAvailableByMemberId(Long memberId) {
        return memberCouponRepository.countAvailableByMemberId(memberId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public long countUsedByMemberId(Long memberId) {
        return memberCouponRepository.countByMemberIdAndStatus(memberId, MemberCouponStatus.USED);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public long countExpiredByMemberId(Long memberId) {
        return memberCouponRepository.countExpiredByMemberId(memberId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Long> getIssuedCouponIds(Long memberId) {
        return memberCouponRepository.findIssuedCouponIdsByMemberId(memberId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<MemberCoupon> getMemberCouponsByCouponId(Long couponId, Pageable pageable) {
        return memberCouponRepository.findAllByCouponId(couponId, pageable);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public MemberCoupon issueCoupon(Long memberId, Long couponId) {
        validateNotAlreadyIssued(memberId, couponId);

        Coupon coupon = couponRepository.findById(couponId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

        coupon.issue();
        couponRepository.save(coupon);

        String couponCode = couponCodeGenerator.generate();
        MemberCoupon memberCoupon = new MemberCoupon(
            memberId, couponId, couponCode, coupon.getValidUntil(), coupon
        );

        return memberCouponRepository.save(memberCoupon);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void useCoupon(Long memberCouponId, Long orderId) {
        MemberCoupon memberCoupon = getMemberCoupon(memberCouponId);
        memberCoupon.use(orderId);
        memberCouponRepository.save(memberCoupon);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void cancelCouponUsage(Long orderId) {
        memberCouponRepository.findByUsedOrderId(orderId)
            .ifPresent(memberCoupon -> {
                memberCoupon.cancelUse();
                memberCouponRepository.save(memberCoupon);
            });
    }

    private void validateNotAlreadyIssued(Long memberId, Long couponId) {
        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.");
        }
    }
}
