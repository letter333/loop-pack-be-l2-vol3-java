package com.loopers.application.coupon;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.kafka.KafkaTopic;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.coupon.IssuableCoupon;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponService;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.domain.coupon.MemberCouponStatusCounts;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.support.auth.AdminValidator;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponFacade {

    private final CouponService couponService;
    private final MemberCouponService memberCouponService;
    private final MemberService memberService;
    private final AdminValidator adminValidator;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

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
        couponService.validateCouponExists(couponId);
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

    public CouponIssueRequestInfo requestCouponIssueAsync(String loginId, String loginPw, Long couponId) {
        Member member = memberService.authenticate(loginId, loginPw);
        couponService.validateCouponExists(couponId);

        String requestId = UUID.randomUUID().toString();

        try {
            Map<String, Object> message = new LinkedHashMap<>();
            message.put("requestId", requestId);
            message.put("memberId", member.getId());
            message.put("couponId", couponId);

            String payload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(KafkaTopic.COUPON_ISSUE_REQUESTS, String.valueOf(couponId), payload).get();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Kafka 메시지 직렬화 실패", e);
        } catch (Exception e) {
            log.error("쿠폰 발급 요청 Kafka 발행 실패: couponId={}, error={}", couponId, e.getMessage());
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE);
        }

        return CouponIssueRequestInfo.pending(requestId, couponId);
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
