package com.loopers.application.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.IssuableCoupon;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponService;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.domain.coupon.MemberCouponStatusCounts;
import com.loopers.domain.member.MemberService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        List<IssuableCoupon> issuableCoupons = couponService.getIssuableCouponsWithIssuedFlag(member.getId());

        return issuableCoupons.stream()
            .map(ic -> CouponInfo.from(ic.coupon(), ic.issued()))
            .toList();
    }

    @Transactional
    public MemberCouponInfo issueCoupon(String loginId, String loginPw, Long couponId) {
        var member = memberService.authenticate(loginId, loginPw);
        MemberCoupon memberCoupon = memberCouponService.issueCoupon(member.getId(), couponId);
        return MemberCouponInfo.from(memberCoupon);
    }

    @Transactional(readOnly = true)
    public MemberCouponListInfo getMyCoupons(String loginId, String loginPw, MemberCouponStatus status, Pageable pageable) {
        var member = memberService.authenticate(loginId, loginPw);
        Long memberId = member.getId();

        Page<MemberCoupon> memberCoupons;
        if (status != null) {
            memberCoupons = memberCouponService.getMemberCouponsByStatus(memberId, status, pageable);
        } else {
            memberCoupons = memberCouponService.getMemberCoupons(memberId, pageable);
        }

        MemberCouponStatusCounts counts = memberCouponService.getStatusCounts(memberId);

        return MemberCouponListInfo.of(memberCoupons, counts.availableCount(), counts.usedCount(), counts.expiredCount());
    }

}
