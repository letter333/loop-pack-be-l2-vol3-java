package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.ProductStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class ProductAdminV1Dto {

    public record CreateProductRequest(
        @NotBlank(message = "상품명은 비어있을 수 없습니다.")
        @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "브랜드 ID는 필수입니다.")
        Long brandId,

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotNull(message = "기본 가격은 필수입니다.")
        @Positive(message = "기본 가격은 0보다 커야 합니다.")
        Long basePrice
    ) {}

    public record UpdateProductRequest(
        @NotBlank(message = "상품명은 비어있을 수 없습니다.")
        @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotNull(message = "기본 가격은 필수입니다.")
        @Positive(message = "기본 가격은 0보다 커야 합니다.")
        Long basePrice,

        Long discount,
        DiscountType discountType,

        @NotNull(message = "상품 상태는 필수입니다.")
        ProductStatus status
    ) {}

    public record ProductDetailResponse(
        Long id,
        String name,
        String productCode,
        Long brandId,
        Long categoryId,
        Long basePrice,
        Long discountedPrice,
        ProductStatus status,
        Long discount,
        DiscountType discountType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
    ) {
        public static ProductDetailResponse from(ProductDetailInfo info) {
            return new ProductDetailResponse(
                info.id(),
                info.name(),
                info.productCode(),
                info.brandId(),
                info.categoryId(),
                info.basePrice(),
                info.discountedPrice(),
                info.status(),
                info.discount(),
                info.discountType(),
                info.createdAt(),
                info.updatedAt(),
                info.deletedAt()
            );
        }
    }
}