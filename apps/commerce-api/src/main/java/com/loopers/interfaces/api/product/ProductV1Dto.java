package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductDetailWithRankingInfo;
import com.loopers.application.product.ProductImageInfo;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductOptionInfo;
import com.loopers.application.ranking.RankingDetailInfo;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.ImageType;
import com.loopers.domain.product.ProductStatus;

import java.util.List;

public class ProductV1Dto {

    public record BrandResponse(
        Long id,
        String name,
        String logoImageUrl
    ) {
        public static BrandResponse from(com.loopers.application.brand.BrandInfo info) {
            return new BrandResponse(
                info.id(),
                info.name(),
                info.logoImageUrl()
            );
        }
    }

    public record OptionResponse(
        Long id,
        String optionValue,
        String displayName,
        Long extraPrice,
        Integer stockQuantity
    ) {
        public static OptionResponse from(ProductOptionInfo info) {
            return new OptionResponse(
                info.id(),
                info.optionValue(),
                info.displayName(),
                info.extraPrice(),
                info.stockQuantity()
            );
        }
    }

    public record ImageResponse(
        Long id,
        ImageType type,
        String url,
        String altText
    ) {
        public static ImageResponse from(ProductImageInfo info) {
            return new ImageResponse(
                info.id(),
                info.type(),
                info.url(),
                info.altText()
            );
        }
    }

    public record ProductResponse(
        Long id,
        String name,
        String productCode,
        Long basePrice,
        Long discountedPrice,
        ProductStatus status,
        Long discount,
        DiscountType discountType,
        BrandResponse brand,
        Long likeCount
    ) {
        public static ProductResponse from(ProductInfo info) {
            return new ProductResponse(
                info.id(),
                info.name(),
                info.productCode(),
                info.basePrice(),
                info.discountedPrice(),
                info.status(),
                info.discount(),
                info.discountType(),
                info.brand() != null ? BrandResponse.from(info.brand()) : null,
                info.likeCount()
            );
        }
    }

    public record RankingResponse(
        Long rank,
        Double score
    ) {
        public static RankingResponse from(RankingDetailInfo info) {
            if (info == null) {
                return null;
            }
            return new RankingResponse(info.rank(), info.score());
        }
    }

    public record ProductDetailResponse(
        Long id,
        String name,
        String productCode,
        Long basePrice,
        Long discountedPrice,
        ProductStatus status,
        Long discount,
        DiscountType discountType,
        BrandResponse brand,
        Long likeCount,
        List<OptionResponse> options,
        List<ImageResponse> images,
        RankingResponse ranking
    ) {
        public static ProductDetailResponse from(ProductDetailWithRankingInfo info) {
            ProductDetailInfo detail = info.productDetail();
            return new ProductDetailResponse(
                detail.id(),
                detail.name(),
                detail.productCode(),
                detail.basePrice(),
                detail.discountedPrice(),
                detail.status(),
                detail.discount(),
                detail.discountType(),
                detail.brand() != null ? BrandResponse.from(detail.brand()) : null,
                detail.likeCount(),
                detail.options().stream().map(OptionResponse::from).toList(),
                detail.images().stream().map(ImageResponse::from).toList(),
                RankingResponse.from(info.ranking())
            );
        }
    }
}