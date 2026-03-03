package com.loopers.interfaces.api.category;

import com.loopers.application.category.CategoryCommand;
import com.loopers.application.category.CategoryDetailInfo;
import com.loopers.application.category.CategoryFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/categories")
public class CategoryAdminV1Controller implements CategoryAdminV1ApiSpec {

    private final CategoryFacade categoryFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse> createCategory(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @Valid @RequestBody CategoryAdminV1Dto.CreateCategoryRequest request
    ) {
        CategoryCommand.Create command = new CategoryCommand.Create(request.name(), request.parentId());
        CategoryDetailInfo info = categoryFacade.createCategory(ldap, command);
        CategoryAdminV1Dto.CategoryDetailResponse response = CategoryAdminV1Dto.CategoryDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PutMapping("/{categoryId}")
    @Override
    public ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse> updateCategory(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long categoryId,
        @Valid @RequestBody CategoryAdminV1Dto.UpdateCategoryRequest request
    ) {
        CategoryCommand.Update command = new CategoryCommand.Update(request.name());
        CategoryDetailInfo info = categoryFacade.updateCategory(ldap, categoryId, command);
        CategoryAdminV1Dto.CategoryDetailResponse response = CategoryAdminV1Dto.CategoryDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{categoryId}")
    @Override
    public ApiResponse<Object> deleteCategory(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long categoryId
    ) {
        categoryFacade.deleteCategory(ldap, categoryId);
        return ApiResponse.success();
    }
}