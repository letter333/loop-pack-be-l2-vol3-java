package com.loopers.domain.coupon;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
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
    public MemberCouponStatusCounts getStatusCounts(Long memberId) {
        return memberCouponRepository.countStatusesByMemberId(memberId);
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

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public MemberCoupon validateAndGetCoupon(Long memberCouponId, Long memberId) {
        MemberCoupon memberCoupon = getMemberCouponWithCoupon(memberCouponId);

        if (!memberCoupon.isOwnedBy(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 쿠폰에 대한 권한이 없습니다.");
        }
        if (!memberCoupon.isAvailable()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }

        if (memberCoupon.getCoupon() == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "쿠폰 정보를 찾을 수 없습니다.");
        }

        return memberCoupon;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void useCoupon(MemberCoupon memberCoupon, Long orderId) {
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
