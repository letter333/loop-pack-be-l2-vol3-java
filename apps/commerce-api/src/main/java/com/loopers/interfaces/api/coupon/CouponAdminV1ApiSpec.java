package com.loopers.interfaces.api.coupon;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Coupon Admin V1 API", description = "쿠폰 관리자 API 입니다.")
public interface CouponAdminV1ApiSpec {

    @Operation(
        summary = "쿠폰 템플릿 목록 조회 (Admin)",
        description = "관리자용 쿠폰 템플릿 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    ApiResponse<Page<CouponAdminV1Dto.CouponDetailResponse>> getCoupons(String ldap, Pageable pageable);

    @Operation(
        summary = "쿠폰 템플릿 상세 조회 (Admin)",
        description = "관리자용 쿠폰 템플릿 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 없음")
    })
    ApiResponse<CouponAdminV1Dto.CouponDetailResponse> getCoupon(String ldap, Long couponId);

    @Operation(
        summary = "쿠폰 템플릿 생성",
        description = "새로운 쿠폰 템플릿을 생성합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    ApiResponse<CouponAdminV1Dto.CouponDetailResponse> createCoupon(
        String ldap, CouponAdminV1Dto.CreateCouponRequest request
    );

    @Operation(
        summary = "쿠폰 템플릿 수정",
        description = "쿠폰 템플릿 정보를 수정합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 요청"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 없음")
    })
    ApiResponse<CouponAdminV1Dto.CouponDetailResponse> updateCoupon(
        String ldap, Long couponId, CouponAdminV1Dto.UpdateCouponRequest request
    );

    @Operation(
        summary = "쿠폰 템플릿 삭제",
        description = "쿠폰 템플릿을 삭제합니다. (Soft Delete)"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 없음")
    })
    ApiResponse<Object> deleteCoupon(String ldap, Long couponId);

    @Operation(
        summary = "쿠폰 발급 내역 조회 (Admin)",
        description = "특정 쿠폰의 발급 내역을 페이징하여 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 없음")
    })
    ApiResponse<Page<CouponAdminV1Dto.CouponIssueResponse>> getCouponIssues(String ldap, Long couponId, Pageable pageable);
}
