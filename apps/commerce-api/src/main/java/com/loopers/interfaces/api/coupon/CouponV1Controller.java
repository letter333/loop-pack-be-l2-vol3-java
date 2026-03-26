package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.CouponIssueRequestInfo;
import com.loopers.application.coupon.MemberCouponInfo;
import com.loopers.application.coupon.MemberCouponListInfo;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponV1Controller implements CouponV1ApiSpec {

    private final CouponFacade couponFacade;

    @GetMapping
    @Override
    public ApiResponse<List<CouponV1Dto.CouponResponse>> getIssuableCoupons(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String loginPw
    ) {
        List<CouponInfo> infos = couponFacade.getIssuableCoupons(loginId, loginPw);
        List<CouponV1Dto.CouponResponse> response = infos.stream()
            .map(CouponV1Dto.CouponResponse::from)
            .toList();
        return ApiResponse.success(response);
    }

    @PostMapping("/{couponId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<CouponV1Dto.MemberCouponResponse> issueCoupon(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String loginPw,
        @PathVariable Long couponId
    ) {
        MemberCouponInfo info = couponFacade.issueCoupon(loginId, loginPw, couponId);
        CouponV1Dto.MemberCouponResponse response = CouponV1Dto.MemberCouponResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping("/{couponId}/issue/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Override
    public ApiResponse<CouponV1Dto.CouponIssueAsyncResponse> issueCouponAsync(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String loginPw,
        @PathVariable Long couponId
    ) {
        CouponIssueRequestInfo info = couponFacade.requestCouponIssueAsync(loginId, loginPw, couponId);
        return ApiResponse.success(CouponV1Dto.CouponIssueAsyncResponse.from(info));
    }

    @GetMapping("/issue-requests/{requestId}")
    @Override
    public ApiResponse<CouponV1Dto.CouponIssueRequestStatusResponse> getCouponIssueRequestStatus(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String loginPw,
        @PathVariable String requestId
    ) {
        // Sub-step 3-3에서 구현 예정 — 임시로 PENDING 반환
        var info = com.loopers.application.coupon.CouponIssueRequestStatusInfo.pending(requestId);
        return ApiResponse.success(CouponV1Dto.CouponIssueRequestStatusResponse.from(info));
    }

    @GetMapping("/my")
    @Override
    public ApiResponse<CouponV1Dto.MemberCouponListResponse> getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String loginPw,
        @RequestParam(required = false) MemberCouponStatus status,
        Pageable pageable
    ) {
        MemberCouponListInfo info = couponFacade.getMyCoupons(loginId, loginPw, status, pageable);
        CouponV1Dto.MemberCouponListResponse response = CouponV1Dto.MemberCouponListResponse.from(info);
        return ApiResponse.success(response);
    }
}
