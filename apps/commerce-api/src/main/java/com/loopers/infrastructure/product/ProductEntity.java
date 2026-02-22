package com.loopers.infrastructure.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "products")
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
        return entity;
    }

    public Product toDomain() {
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
            getCreatedAt() != null ? getCreatedAt().toLocalDateTime() : null,
            getUpdatedAt() != null ? getUpdatedAt().toLocalDateTime() : null,
            getDeletedAt() != null ? getDeletedAt().toLocalDateTime() : null
        );
    }

    public void update(String name, Long categoryId, Long basePrice,
                       Long discount, DiscountType discountType, ProductStatus status) {
        this.name = name;
        this.categoryId = categoryId;
        this.basePrice = basePrice;
        this.discount = discount;
        this.discountType = discountType;
        this.status = status;
    }
}