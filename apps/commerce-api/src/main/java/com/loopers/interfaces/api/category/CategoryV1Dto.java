package com.loopers.interfaces.api.category;

import com.loopers.application.category.CategoryInfo;

import java.util.List;

public class CategoryV1Dto {

    public record CategoryResponse(
        Long id,
        Long parentId,
        String name,
        Integer depth,
        List<CategoryResponse> children
    ) {
        public static CategoryResponse from(CategoryInfo info) {
            List<CategoryResponse> childResponses = info.children().stream()
                .map(CategoryResponse::from)
                .toList();
            return new CategoryResponse(
                info.id(),
                info.parentId(),
                info.name(),
                info.depth(),
                childResponses
            );
        }
    }
}