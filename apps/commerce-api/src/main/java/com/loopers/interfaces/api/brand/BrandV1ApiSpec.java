package com.loopers.interfaces.api.brand;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Brand V1 API", description = "브랜드 API 입니다.")
public interface BrandV1ApiSpec {

    @Operation(
        summary = "브랜드 목록 조회",
        description = "활성 브랜드 목록을 페이징하여 조회합니다."
    )
    ApiResponse<Page<BrandV1Dto.BrandResponse>> getBrands(Pageable pageable);

    @Operation(
        summary = "브랜드 상세 조회",
        description = "브랜드 상세 정보를 조회합니다."
    )
    ApiResponse<BrandV1Dto.BrandResponse> getBrand(Long brandId);
}