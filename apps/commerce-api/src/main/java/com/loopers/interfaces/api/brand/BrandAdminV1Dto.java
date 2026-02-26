package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandDetailInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class BrandAdminV1Dto {

    public record CreateBrandRequest(
        @NotBlank(message = "브랜드명은 비어있을 수 없습니다.")
        @Size(max = 50, message = "브랜드명은 50자를 초과할 수 없습니다.")
        String name,

        String description,

        @Size(max = 512, message = "로고 이미지 URL은 512자를 초과할 수 없습니다.")
        String logoImageUrl
    ) {}

    public record UpdateBrandRequest(
        @NotBlank(message = "브랜드명은 비어있을 수 없습니다.")
        @Size(max = 50, message = "브랜드명은 50자를 초과할 수 없습니다.")
        String name,

        String description,

        @Size(max = 512, message = "로고 이미지 URL은 512자를 초과할 수 없습니다.")
        String logoImageUrl
    ) {}

    public record BrandDetailResponse(
        Long id,
        String name,
        String description,
        String logoImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        public static BrandDetailResponse from(BrandDetailInfo info) {
            return new BrandDetailResponse(
                info.id(),
                info.name(),
                info.description(),
                info.logoImageUrl(),
                info.createdAt(),
                info.updatedAt(),
                info.deletedAt()
            );
        }
    }
}