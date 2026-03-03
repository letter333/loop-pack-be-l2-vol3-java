package com.loopers.application.category;

import com.loopers.domain.category.Category;

import java.util.List;

public record CategoryInfo(
    Long id,
    Long parentId,
    String name,
    Integer depth,
    List<CategoryInfo> children
) {
    public static CategoryInfo from(Category category) {
        return new CategoryInfo(
            category.getId(),
            category.getParentId(),
            category.getName(),
            category.getDepth(),
            List.of()
        );
    }

    public static CategoryInfo withChildren(Category category, List<CategoryInfo> children) {
        return new CategoryInfo(
            category.getId(),
            category.getParentId(),
            category.getName(),
            category.getDepth(),
            children
        );
    }
}