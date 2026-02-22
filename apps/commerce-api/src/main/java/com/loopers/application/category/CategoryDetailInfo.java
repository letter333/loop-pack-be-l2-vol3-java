package com.loopers.application.category;

import com.loopers.domain.category.Category;

import java.time.LocalDateTime;

public record CategoryDetailInfo(
    Long id,
    Long parentId,
    String name,
    String path,
    Integer depth,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public static CategoryDetailInfo from(Category category) {
        return new CategoryDetailInfo(
            category.getId(),
            category.getParentId(),
            category.getName(),
            category.getPath(),
            category.getDepth(),
            category.getCreatedAt(),
            category.getUpdatedAt(),
            category.getDeletedAt()
        );
    }
}