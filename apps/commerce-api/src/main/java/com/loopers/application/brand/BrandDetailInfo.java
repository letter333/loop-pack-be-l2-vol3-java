package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;

import java.time.LocalDateTime;

public record BrandDetailInfo(
    Long id,
    String name,
    String description,
    String logoImageUrl,
    Long likeCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime deletedAt
) {
    public static BrandDetailInfo from(Brand brand) {
        return new BrandDetailInfo(
            brand.getId(),
            brand.getName(),
            brand.getDescription(),
            brand.getLogoImageUrl(),
            brand.getLikeCount(),
            brand.getCreatedAt(),
            brand.getUpdatedAt(),
            brand.getDeletedAt()
        );
    }
}