package com.loopers.interfaces.api.like;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Like V1 API", description = "좋아요 API 입니다.")
public interface LikeV1ApiSpec {

    @Operation(
        summary = "상품 좋아요 토글",
        description = "상품에 좋아요를 추가하거나 취소합니다. 좋아요가 없으면 추가하고, 있으면 취소합니다."
    )
    ApiResponse<LikeV1Dto.LikeResponse> toggleProductLike(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "상품 ID", required = true) Long productId
    );

    @Operation(
        summary = "브랜드 좋아요 토글",
        description = "브랜드에 좋아요를 추가하거나 취소합니다. 좋아요가 없으면 추가하고, 있으면 취소합니다."
    )
    ApiResponse<LikeV1Dto.LikeResponse> toggleBrandLike(
        @Parameter(description = "로그인 ID", required = true) String loginId,
        @Parameter(description = "비밀번호", required = true) String password,
        @Parameter(description = "브랜드 ID", required = true) Long brandId
    );
}
