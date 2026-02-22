package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Product V1 API", description = "상품 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "활성 상품 목록을 조회합니다. 브랜드 정보와 좋아요 수를 포함합니다."
    )
    ApiResponse<List<ProductV1Dto.ProductResponse>> getProducts();

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 상세 정보를 조회합니다. 브랜드 정보와 좋아요 수를 포함합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(Long productId);
}