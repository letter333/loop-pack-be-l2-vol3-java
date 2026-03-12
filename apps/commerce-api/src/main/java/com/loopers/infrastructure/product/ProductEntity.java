package com.loopers.infrastructure.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductImage;
import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_brand_created", columnList = "brand_id, created_at DESC"),
    @Index(name = "idx_products_category_created", columnList = "category_id, created_at DESC"),
    @Index(name = "idx_products_like_count", columnList = "like_count DESC, created_at DESC"),
    @Index(name = "idx_products_base_price", columnList = "base_price ASC, created_at DESC"),
    @Index(name = "idx_products_created", columnList = "created_at DESC")
})
@SQLDelete(sql = "UPDATE products SET deleted_at = NOW() WHERE id = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "product_code", nullable = false, unique = true, length = 20)
    private String productCode;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "base_price", nullable = false)
    private Long basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ProductStatus status;

    @Column(name = "discount")
    private Long discount;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductOptionEntity> options = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductImageEntity> images = new HashSet<>();

    public static ProductEntity from(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.name = product.getName();
        entity.productCode = product.getProductCode();
        entity.brandId = product.getBrandId();
        entity.categoryId = product.getCategoryId();
        entity.basePrice = product.getBasePrice();
        entity.status = product.getStatus();
        entity.discount = product.getDiscount();
        entity.discountType = product.getDiscountType();
        entity.likeCount = product.getLikeCount() != null ? product.getLikeCount() : 0L;

        if (product.getOptions() != null) {
            for (ProductOption option : product.getOptions()) {
                entity.addOption(ProductOptionEntity.from(option));
            }
        }
        if (product.getImages() != null) {
            for (ProductImage image : product.getImages()) {
                entity.addImage(ProductImageEntity.from(image));
            }
        }

        return entity;
    }

    public void addOption(ProductOptionEntity option) {
        options.add(option);
        option.setProduct(this);
    }

    public void removeOption(ProductOptionEntity option) {
        options.remove(option);
        option.setProduct(null);
    }

    public void addImage(ProductImageEntity image) {
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImageEntity image) {
        images.remove(image);
        image.setProduct(null);
    }

    public Product toDomain() {
        List<ProductOption> domainOptions = options.stream()
            .filter(opt -> opt.getDeletedAt() == null)
            .map(ProductOptionEntity::toDomain)
            .toList();

        List<ProductImage> domainImages = images.stream()
            .map(ProductImageEntity::toDomain)
            .toList();

        return new Product(
            getId(),
            name,
            productCode,
            brandId,
            categoryId,
            basePrice,
            status,
            discount,
            discountType,
            likeCount,
            domainOptions,
            domainImages,
            getCreatedAt() != null ? getCreatedAt().toLocalDateTime() : null,
            getUpdatedAt() != null ? getUpdatedAt().toLocalDateTime() : null,
            getDeletedAt() != null ? getDeletedAt().toLocalDateTime() : null
        );
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void update(String name, Long categoryId, Long basePrice,
                       Long discount, DiscountType discountType, ProductStatus status, Long likeCount) {
        this.name = name;
        this.categoryId = categoryId;
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.basePrice = basePrice;
        this.discount = discount;
        this.discountType = discountType;
        this.status = status;
    }

    public void syncOptions(List<ProductOption> newOptions) {
        if (newOptions == null || newOptions.isEmpty()) {
            this.options.clear();
            return;
        }

        // Update existing options and track which IDs we've seen
        Set<Long> updatedIds = new HashSet<>();
        for (ProductOption domainOption : newOptions) {
            if (domainOption.getId() != null) {
                ProductOptionEntity existingEntity = findOptionById(domainOption.getId());
                if (existingEntity != null) {
                    existingEntity.updateStockQuantity(domainOption.getStockQuantity());
                    updatedIds.add(domainOption.getId());
                }
            } else {
                // New option without ID
                addOption(ProductOptionEntity.from(domainOption));
            }
        }

        // Remove options that are no longer in the domain list
        this.options.removeIf(opt -> opt.getId() != null && !updatedIds.contains(opt.getId()));
    }

    public void syncImages(List<ProductImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            for (ProductImage image : newImages) {
                addImage(ProductImageEntity.from(image));
            }
        }
    }

    public ProductOptionEntity findOptionById(Long optionId) {
        return options.stream()
            .filter(opt -> opt.getId().equals(optionId))
            .findFirst()
            .orElse(null);
    }
}