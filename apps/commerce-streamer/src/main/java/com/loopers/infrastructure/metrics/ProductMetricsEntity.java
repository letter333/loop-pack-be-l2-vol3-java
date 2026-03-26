package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "product_metrics")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsEntity {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void onUpdate() {
        this.updatedAt = ZonedDateTime.now();
    }

    public static ProductMetricsEntity from(ProductMetrics metrics) {
        ProductMetricsEntity entity = new ProductMetricsEntity();
        entity.productId = metrics.getProductId();
        entity.likeCount = metrics.getLikeCount();
        entity.viewCount = metrics.getViewCount();
        entity.salesCount = metrics.getSalesCount();
        entity.salesAmount = metrics.getSalesAmount();
        return entity;
    }

    public ProductMetrics toDomain() {
        return new ProductMetrics(
            productId, likeCount, viewCount, salesCount, salesAmount,
            updatedAt
        );
    }
}
