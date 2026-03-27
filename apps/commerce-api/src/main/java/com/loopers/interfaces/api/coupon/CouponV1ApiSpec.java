package com.loopers.interfaces.api.coupon;

import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Tag(name = "Coupon V1 API", description = "쿠폰 API 입니다.")
public interface CouponV1ApiSpec {

    @Operation(
        summary = "발급 가능한 쿠폰 목록 조회",
        description = "현재 발급 가능한 쿠폰 목록을 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<List<CouponV1Dto.CouponResponse>> getIssuableCoupons(String loginId, String loginPw);

    @Operation(
        summary = "쿠폰 발급",
        description = "쿠폰을 발급받습니다. 랜덤 쿠폰 코드가 생성됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "발급 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "발급 불가 (기간 외, 수량 소진)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 발급받은 쿠폰")
    })
    ApiResponse<CouponV1Dto.MemberCouponResponse> issueCoupon(String loginId, String loginPw, Long couponId);

    @Operation(
        summary = "선착순 쿠폰 발급 요청 (비동기)",
        description = "쿠폰 발급 요청을 Kafka로 발행합니다. 발급 결과는 requestId로 폴링하여 확인합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "요청 접수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "쿠폰 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Kafka 발행 실패")
    })
    ApiResponse<CouponV1Dto.CouponIssueAsyncResponse> issueCouponAsync(String loginId, String loginPw, Long couponId);

    @Operation(
        summary = "쿠폰 발급 요청 상태 조회",
        description = "비동기 쿠폰 발급 요청의 처리 상태를 조회합니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<CouponV1Dto.CouponIssueRequestStatusResponse> getCouponIssueRequestStatus(
        String loginId, String loginPw, String requestId
    );

    @Operation(
        summary = "내 쿠폰 목록 조회",
        description = "발급받은 쿠폰 목록을 조회합니다. status 파라미터로 필터링할 수 있습니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    ApiResponse<CouponV1Dto.MemberCouponListResponse> getMyCoupons(
        String loginId, String loginPw, MemberCouponStatus status, Pageable pageable
    );
}
