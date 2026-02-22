package com.loopers.infrastructure.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.ProductOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "product_options")
@SQLDelete(sql = "UPDATE product_options SET deleted_at = NOW() WHERE id = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOptionEntity extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "option_value", nullable = false, length = 50)
    private String optionValue;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "extra_price")
    private Long extraPrice;

    @Column(name = "stock_quantity")
    private Integer stockQuantity;

    public static ProductOptionEntity from(ProductOption productOption) {
        ProductOptionEntity entity = new ProductOptionEntity();
        entity.productId = productOption.getProductId();
        entity.optionValue = productOption.getOptionValue();
        entity.displayName = productOption.getDisplayName();
        entity.extraPrice = productOption.getExtraPrice();
        entity.stockQuantity = productOption.getStockQuantity();
        return entity;
    }

    public ProductOption toDomain() {
        return new ProductOption(
            getId(),
            productId,
            optionValue,
            displayName,
            extraPrice,
            stockQuantity,
            getCreatedAt() != null ? getCreatedAt().toLocalDateTime() : null,
            getUpdatedAt() != null ? getUpdatedAt().toLocalDateTime() : null,
            getDeletedAt() != null ? getDeletedAt().toLocalDateTime() : null
        );
    }

    public void updateStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }
}