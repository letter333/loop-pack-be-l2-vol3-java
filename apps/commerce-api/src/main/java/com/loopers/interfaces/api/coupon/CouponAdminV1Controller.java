package com.loopers.interfaces.api.coupon;

import com.loopers.application.coupon.CouponCommand;
import com.loopers.application.coupon.CouponDetailInfo;
import com.loopers.application.coupon.CouponFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/coupons")
public class CouponAdminV1Controller implements CouponAdminV1ApiSpec {

    private final CouponFacade couponFacade;

    @GetMapping
    @Override
    public ApiResponse<Page<CouponAdminV1Dto.CouponDetailResponse>> getCoupons(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        Pageable pageable
    ) {
        Page<CouponDetailInfo> infos = couponFacade.getCouponsForAdmin(ldap, pageable);
        Page<CouponAdminV1Dto.CouponDetailResponse> response = infos.map(CouponAdminV1Dto.CouponDetailResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/{couponId}")
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponDetailResponse> getCoupon(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long couponId
    ) {
        CouponDetailInfo info = couponFacade.getCouponDetail(ldap, couponId);
        CouponAdminV1Dto.CouponDetailResponse response = CouponAdminV1Dto.CouponDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponDetailResponse> createCoupon(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @Valid @RequestBody CouponAdminV1Dto.CreateCouponRequest request
    ) {
        CouponCommand.Create command = new CouponCommand.Create(
            request.name(),
            request.description(),
            request.couponType(),
            request.discountValue(),
            request.minOrderAmount(),
            request.maxDiscountAmount(),
            request.totalQuantity(),
            request.validFrom(),
            request.validUntil()
        );
        CouponDetailInfo info = couponFacade.createCoupon(ldap, command);
        CouponAdminV1Dto.CouponDetailResponse response = CouponAdminV1Dto.CouponDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PutMapping("/{couponId}")
    @Override
    public ApiResponse<CouponAdminV1Dto.CouponDetailResponse> updateCoupon(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long couponId,
        @Valid @RequestBody CouponAdminV1Dto.UpdateCouponRequest request
    ) {
        CouponCommand.Update command = new CouponCommand.Update(
            request.name(),
            request.description(),
            request.couponType(),
            request.discountValue(),
            request.minOrderAmount(),
            request.maxDiscountAmount(),
            request.totalQuantity(),
            request.validFrom(),
            request.validUntil()
        );
        CouponDetailInfo info = couponFacade.updateCoupon(ldap, couponId, command);
        CouponAdminV1Dto.CouponDetailResponse response = CouponAdminV1Dto.CouponDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{couponId}")
    @Override
    public ApiResponse<Object> deleteCoupon(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long couponId
    ) {
        couponFacade.deleteCoupon(ldap, couponId);
        return ApiResponse.success();
    }
}
