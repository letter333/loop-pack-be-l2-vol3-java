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
    public List<MemberCoupon> getMemberCoupons(Long memberId) {
        return memberCouponRepository.findAllByMemberId(memberId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<MemberCoupon> getMemberCouponsByStatus(Long memberId, MemberCouponStatus status) {
        return memberCouponRepository.findAllByMemberIdAndStatus(memberId, status);
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
            memberId, couponId, couponCode, coupon.getValidUntil()
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

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public void validateCouponOwnership(Long memberCouponId, Long memberId) {
        MemberCoupon memberCoupon = getMemberCoupon(memberCouponId);
        if (!memberCoupon.isOwnedBy(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 쿠폰에 대한 권한이 없습니다.");
        }
    }

    private void validateNotAlreadyIssued(Long memberId, Long couponId) {
        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.");
        }
    }
}
