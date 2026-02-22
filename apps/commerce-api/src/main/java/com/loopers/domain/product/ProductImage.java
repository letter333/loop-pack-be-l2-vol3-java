package com.loopers.domain.product;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductImage {

    private Long id;
    private Long productId;
    private ImageType type;
    private String url;
    private String altText;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ProductImage(Long productId, ImageType type, String url, String altText) {
        ProductImageValidator.validateProductId(productId);
        ProductImageValidator.validateUrl(url);

        this.productId = productId;
        this.type = type;
        this.url = url;
        this.altText = altText;
    }

    public ProductImage(Long id, Long productId, ImageType type, String url, String altText,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.productId = productId;
        this.type = type;
        this.url = url;
        this.altText = altText;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
