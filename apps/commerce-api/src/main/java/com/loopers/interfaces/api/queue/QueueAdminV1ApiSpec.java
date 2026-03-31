package com.loopers.interfaces.api.queue;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Queue Admin V1 API", description = "대기열 관리자 API 입니다.")
public interface QueueAdminV1ApiSpec {

    @Operation(summary = "대기열 활성화", description = "주문 대기열을 활성화합니다.")
    ApiResponse<Object> activate(
        @Parameter(description = "관리자 LDAP", required = true) String ldap
    );

    @Operation(summary = "대기열 비활성화", description = "주문 대기열을 비활성화합니다.")
    ApiResponse<Object> deactivate(
        @Parameter(description = "관리자 LDAP", required = true) String ldap
    );

    @Operation(summary = "대기열 상태 조회", description = "현재 대기열 활성화 상태를 조회합니다.")
    ApiResponse<QueueV1Dto.QueueStatusResponse> getStatus(
        @Parameter(description = "관리자 LDAP", required = true) String ldap
    );
}
