package com.loopers.interfaces.api.category;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Category Admin V1 API", description = "카테고리 관리자 API 입니다.")
public interface CategoryAdminV1ApiSpec {

    @Operation(
        summary = "카테고리 등록 (Admin)",
        description = "새로운 카테고리를 등록합니다."
    )
    ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse> createCategory(
        String ldap,
        CategoryAdminV1Dto.CreateCategoryRequest request
    );

    @Operation(
        summary = "카테고리 수정 (Admin)",
        description = "카테고리 정보를 수정합니다."
    )
    ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse> updateCategory(
        String ldap,
        Long categoryId,
        CategoryAdminV1Dto.UpdateCategoryRequest request
    );

    @Operation(
        summary = "카테고리 삭제 (Admin)",
        description = "카테고리를 삭제합니다. 하위 카테고리와 소속 상품도 함께 삭제됩니다."
    )
    ApiResponse<Object> deleteCategory(String ldap, Long categoryId);
}