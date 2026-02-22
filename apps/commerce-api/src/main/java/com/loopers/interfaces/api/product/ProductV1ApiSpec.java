package com.loopers.interfaces.api.product;

import com.loopers.domain.product.ProductSortType;
import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Product V1 API", description = "상품 API 입니다.")
public interface ProductV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회",
        description = "활성 상품 목록을 조회합니다. 브랜드 정보와 좋아요 수를 포함합니다. 검색, 정렬, 페이징을 지원합니다."
    )
    ApiResponse<Page<ProductV1Dto.ProductResponse>> getProducts(
        @Parameter(description = "카테고리 ID (null이면 전체)") Long categoryId,
        @Parameter(description = "검색 키워드 (상품명 검색)") String keyword,
        @Parameter(description = "정렬 기준 (LATEST: 최신순, PRICE_ASC: 가격 낮은순, LIKES_DESC: 좋아요순)") ProductSortType sort,
        @Parameter(description = "페이징 정보") Pageable pageable
    );

    @Operation(
        summary = "상품 상세 조회",
        description = "상품 상세 정보를 조회합니다. 브랜드 정보와 좋아요 수를 포함합니다."
    )
    ApiResponse<ProductV1Dto.ProductResponse> getProduct(Long productId);
}