package com.loopers.interfaces.api.category;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Category V1 API", description = "카테고리 API 입니다.")
public interface CategoryV1ApiSpec {

    @Operation(
        summary = "카테고리 목록 조회",
        description = "계층 구조로 카테고리 목록을 조회합니다."
    )
    ApiResponse<List<CategoryV1Dto.CategoryResponse>> getCategories();
}