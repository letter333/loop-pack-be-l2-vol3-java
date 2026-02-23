package com.loopers.interfaces.api.address;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Address V1 API", description = "배송지 관리 API 입니다.")
public interface AddressV1ApiSpec {

    @Operation(
        summary = "배송지 목록 조회",
        description = "회원의 배송지 목록을 조회합니다. 기본 배송지가 먼저 표시됩니다."
    )
    ApiResponse<List<AddressV1Dto.AddressResponse>> getAddresses(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password
    );

    @Operation(
        summary = "배송지 등록",
        description = "새로운 배송지를 등록합니다. 첫 번째 배송지는 자동으로 기본 배송지로 설정됩니다. 최대 5개까지 등록 가능합니다."
    )
    ApiResponse<AddressV1Dto.AddressResponse> registerAddress(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        AddressV1Dto.RegisterRequest request
    );

    @Operation(
        summary = "배송지 수정",
        description = "기존 배송지 정보를 수정합니다."
    )
    ApiResponse<AddressV1Dto.AddressResponse> updateAddress(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "배송지 ID", required = true) Long addressId,
        AddressV1Dto.UpdateRequest request
    );

    @Operation(
        summary = "배송지 삭제",
        description = "배송지를 삭제합니다."
    )
    ApiResponse<Void> deleteAddress(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "배송지 ID", required = true) Long addressId
    );

    @Operation(
        summary = "기본 배송지 설정",
        description = "해당 배송지를 기본 배송지로 설정합니다. 기존 기본 배송지는 자동으로 해제됩니다."
    )
    ApiResponse<AddressV1Dto.AddressResponse> setDefaultAddress(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "배송지 ID", required = true) Long addressId
    );
}
