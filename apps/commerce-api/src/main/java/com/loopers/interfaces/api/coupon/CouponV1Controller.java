package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.application.coupon.CouponInfo;
import com.loopers.application.coupon.MemberCouponInfo;
import com.loopers.application.coupon.MemberCouponListInfo;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
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

    @GetMapping("/my")
    @Override
    public ApiResponse<CouponV1Dto.MemberCouponListResponse> getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String loginPw,
        @RequestParam(required = false) MemberCouponStatus status
    ) {
        MemberCouponListInfo info = couponFacade.getMyCoupons(loginId, loginPw, status);
        CouponV1Dto.MemberCouponListResponse response = CouponV1Dto.MemberCouponListResponse.from(info);
        return ApiResponse.success(response);
    }
}
