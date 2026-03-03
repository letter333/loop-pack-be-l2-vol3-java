package com.loopers.interfaces.api.category;

import com.loopers.application.category.CategoryFacade;
import com.loopers.application.category.CategoryInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/categories")
public class CategoryV1Controller implements CategoryV1ApiSpec {

    private final CategoryFacade categoryFacade;

    @GetMapping
    @Override
    public ApiResponse<List<CategoryV1Dto.CategoryResponse>> getCategories() {
        List<CategoryInfo> infos = categoryFacade.getCategoriesHierarchy();
        List<CategoryV1Dto.CategoryResponse> response = infos.stream()
            .map(CategoryV1Dto.CategoryResponse::from)
            .toList();
        return ApiResponse.success(response);
    }
}