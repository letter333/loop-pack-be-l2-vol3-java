package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponService;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.domain.member.MemberService;
import com.loopers.support.auth.AdminValidator;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponService couponService;
    private final MemberCouponService memberCouponService;
    private final MemberService memberService;
    private final AdminValidator adminValidator;

    @Transactional(readOnly = true)
    public Page<CouponDetailInfo> getCouponsForAdmin(String ldap, Pageable pageable) {
        adminValidator.validate(ldap);
        return couponService.getCouponsForAdmin(pageable)
            .map(CouponDetailInfo::from);
    }

    @Transactional(readOnly = true)
    public CouponDetailInfo getCouponDetail(String ldap, Long couponId) {
        adminValidator.validate(ldap);
        Coupon coupon = couponService.getActiveCoupon(couponId);
        return CouponDetailInfo.from(coupon);
    }

    @Transactional
    public CouponDetailInfo createCoupon(String ldap, CouponCommand.Create command) {
        adminValidator.validate(ldap);
        Coupon coupon = couponService.createCoupon(
            command.name(),
            command.description(),
            command.couponType(),
            command.discountValue(),
            command.minOrderAmount(),
            command.maxDiscountAmount(),
            command.totalQuantity(),
            command.validFrom(),
            command.validUntil()
        );
        return CouponDetailInfo.from(coupon);
    }

    @Transactional
    public CouponDetailInfo updateCoupon(String ldap, Long couponId, CouponCommand.Update command) {
        adminValidator.validate(ldap);
        Coupon coupon = couponService.updateCoupon(
            couponId,
            command.name(),
            command.description(),
            command.couponType(),
            command.discountValue(),
            command.minOrderAmount(),
            command.maxDiscountAmount(),
            command.totalQuantity(),
            command.validFrom(),
            command.validUntil()
        );
        return CouponDetailInfo.from(coupon);
    }

    @Transactional
    public void deleteCoupon(String ldap, Long couponId) {
        adminValidator.validate(ldap);
        couponService.deleteCoupon(couponId);
    }

    @Transactional(readOnly = true)
    public Page<CouponIssueInfo> getCouponIssues(String ldap, Long couponId, Pageable pageable) {
        adminValidator.validate(ldap);
        couponService.getActiveCoupon(couponId);
        return memberCouponService.getMemberCouponsByCouponId(couponId, pageable)
            .map(CouponIssueInfo::from);
    }

    @Transactional(readOnly = true)
    public List<CouponInfo> getIssuableCoupons(String loginId, String loginPw) {
        var member = memberService.authenticate(loginId, loginPw);
        List<Coupon> coupons = couponService.getIssuableCoupons();
        Set<Long> issuedCouponIds = Set.copyOf(memberCouponService.getIssuedCouponIds(member.getId()));

        return coupons.stream()
            .map(coupon -> CouponInfo.from(coupon, issuedCouponIds.contains(coupon.getId())))
            .toList();
    }

    @Transactional
    public MemberCouponInfo issueCoupon(String loginId, String loginPw, Long couponId) {
        var member = memberService.authenticate(loginId, loginPw);
        MemberCoupon memberCoupon = memberCouponService.issueCoupon(member.getId(), couponId);
        return MemberCouponInfo.from(memberCoupon);
    }

    @Transactional(readOnly = true)
    public MemberCouponListInfo getMyCoupons(String loginId, String loginPw, MemberCouponStatus status) {
        var member = memberService.authenticate(loginId, loginPw);

        List<MemberCoupon> memberCoupons;
        if (status != null) {
            memberCoupons = memberCouponService.getMemberCouponsByStatus(member.getId(), status);
        } else {
            memberCoupons = memberCouponService.getMemberCoupons(member.getId());
        }

        return MemberCouponListInfo.from(memberCoupons);
    }

    @Transactional(readOnly = true)
    public Long calculateCouponDiscount(Long memberCouponId, Long memberId, Long orderAmount) {
        MemberCoupon memberCoupon = memberCouponService.getMemberCouponWithCoupon(memberCouponId);

        if (!memberCoupon.isOwnedBy(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 쿠폰에 대한 권한이 없습니다.");
        }

        if (!memberCoupon.isAvailable()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다.");
        }

        Coupon coupon = memberCoupon.getCoupon();
        if (coupon == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "쿠폰 정보를 찾을 수 없습니다.");
        }

        return coupon.calculateDiscount(orderAmount);
    }

    @Transactional
    public void applyCoupon(Long memberCouponId, Long orderId) {
        memberCouponService.useCoupon(memberCouponId, orderId);
    }

    @Transactional
    public void cancelCouponUsage(Long orderId) {
        memberCouponService.cancelCouponUsage(orderId);
    }
}
