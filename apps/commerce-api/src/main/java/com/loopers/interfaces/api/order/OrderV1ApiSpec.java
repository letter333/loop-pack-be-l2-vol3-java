package com.loopers.interfaces.api.order;

import com.loopers.domain.order.OrderPeriod;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Order V1 API", description = "주문 관리 API 입니다.")
public interface OrderV1ApiSpec {

    @Operation(
        summary = "주문 생성",
        description = "새로운 주문을 생성합니다. 배송지와 주문 상품 정보가 필요합니다."
    )
    ApiResponse<OrderV1Dto.OrderDetailResponse> createOrder(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "대기열 입장 토큰 (대기열 활성 시 필수)") String queueToken,
        OrderV1Dto.CreateOrderRequest request
    );

    @Operation(
        summary = "주문 목록 조회",
        description = "회원의 주문 목록을 조회합니다. 기간 필터를 적용할 수 있습니다."
    )
    ApiResponse<List<OrderV1Dto.OrderResponse>> getOrders(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "조회 기간") OrderPeriod period
    );

    @Operation(
        summary = "주문 상세 조회",
        description = "주문 상세 정보를 조회합니다."
    )
    ApiResponse<OrderV1Dto.OrderDetailResponse> getOrderDetail(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "주문 ID", required = true) Long orderId
    );

    @Operation(
        summary = "주문 취소",
        description = "주문을 취소합니다. 결제 대기 또는 결제 완료 상태에서만 취소 가능합니다."
    )
    ApiResponse<OrderV1Dto.OrderDetailResponse> cancelOrder(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "주문 ID", required = true) Long orderId
    );
}
