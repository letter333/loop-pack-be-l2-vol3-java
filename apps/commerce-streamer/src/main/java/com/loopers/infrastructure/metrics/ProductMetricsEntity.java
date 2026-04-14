package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Table(name = "product_metrics", indexes = {
    @Index(name = "idx_metric_date", columnList = "metric_date")
})
@IdClass(ProductMetricsId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMetricsEntity {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Id
    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

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
        entity.metricDate = metrics.getMetricDate();
        entity.likeCount = metrics.getLikeCount();
        entity.viewCount = metrics.getViewCount();
        entity.salesCount = metrics.getSalesCount();
        entity.salesAmount = metrics.getSalesAmount();
        return entity;
    }

    public ProductMetrics toDomain() {
        return new ProductMetrics(
            productId, metricDate, likeCount, viewCount, salesCount, salesAmount,
            updatedAt
        );
    }
}
