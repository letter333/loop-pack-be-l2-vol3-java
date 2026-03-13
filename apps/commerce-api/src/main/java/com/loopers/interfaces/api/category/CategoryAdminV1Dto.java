package com.loopers.interfaces.api.category;

import com.loopers.application.category.CategoryDetailInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class CategoryAdminV1Dto {

    public record CreateCategoryRequest(
        @NotBlank(message = "카테고리명은 비어있을 수 없습니다.")
        @Size(max = 20, message = "카테고리명은 20자를 초과할 수 없습니다.")
        String name,

        Long parentId
    ) {}

    public record UpdateCategoryRequest(
        @NotBlank(message = "카테고리명은 비어있을 수 없습니다.")
        @Size(max = 20, message = "카테고리명은 20자를 초과할 수 없습니다.")
        String name
    ) {}

    public record CategoryDetailResponse(
        Long id,
        Long parentId,
        String name,
        String path,
        Integer depth,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        public static CategoryDetailResponse from(CategoryDetailInfo info) {
            return new CategoryDetailResponse(
                info.id(),
                info.parentId(),
                info.name(),
                info.path(),
                info.depth(),
                info.createdAt(),
                info.updatedAt(),
                info.deletedAt()
            );
        }
    }
}