package com.loopers.domain.metrics;

import lombok.Getter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Getter
public class ProductMetrics {

    private Long productId;
    private LocalDate metricDate;
    private Long likeCount;
    private Long viewCount;
    private Long salesCount;
    private Long salesAmount;
    private ZonedDateTime updatedAt;

    public ProductMetrics(Long productId) {
        this.productId = productId;
        this.metricDate = LocalDate.now();
        this.likeCount = 0L;
        this.viewCount = 0L;
        this.salesCount = 0L;
        this.salesAmount = 0L;
        this.updatedAt = ZonedDateTime.now();
    }

    public ProductMetrics(Long productId, LocalDate metricDate, Long likeCount, Long viewCount,
                          Long salesCount, Long salesAmount, ZonedDateTime updatedAt) {
        this.productId = productId;
        this.metricDate = metricDate;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
        this.updatedAt = updatedAt;
    }
}
