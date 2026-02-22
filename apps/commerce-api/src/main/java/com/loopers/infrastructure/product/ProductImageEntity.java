package com.loopers.infrastructure.product;

import com.loopers.domain.product.ImageType;
import com.loopers.domain.product.ProductImage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20)
    private ImageType type;

    @Column(name = "url", nullable = false, length = 512)
    private String url;

    @Column(name = "alt_text", length = 255)
    private String altText;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static ProductImageEntity from(ProductImage productImage) {
        ProductImageEntity entity = new ProductImageEntity();
        entity.productId = productImage.getProductId();
        entity.type = productImage.getType();
        entity.url = productImage.getUrl();
        entity.altText = productImage.getAltText();
        return entity;
    }

    public ProductImage toDomain() {
        return new ProductImage(
            id,
            productId,
            type,
            url,
            altText,
            createdAt,
            updatedAt
        );
    }
}