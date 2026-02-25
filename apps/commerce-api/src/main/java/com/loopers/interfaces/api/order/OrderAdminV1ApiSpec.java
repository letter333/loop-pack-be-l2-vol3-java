package com.loopers.interfaces.api.order;

import com.loopers.domain.order.OrderPeriod;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Order Admin V1 API", description = "관리자용 주문 관리 API 입니다.")
public interface OrderAdminV1ApiSpec {

    @Operation(
        summary = "[Admin] 주문 목록 조회",
        description = "전체 주문 목록을 조회합니다. 관리자 권한이 필요합니다."
    )
    ApiResponse<List<OrderAdminV1Dto.OrderAdminResponse>> getOrders(
        @Parameter(description = "LDAP 인증 헤더", required = true) String ldap,
        @Parameter(description = "조회 기간") OrderPeriod period
    );

    @Operation(
        summary = "[Admin] 주문 상세 조회",
        description = "주문 상세 정보를 조회합니다. 관리자 권한이 필요합니다."
    )
    ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse> getOrderDetail(
        @Parameter(description = "LDAP 인증 헤더", required = true) String ldap,
        @Parameter(description = "주문 ID", required = true) Long orderId
    );
}
