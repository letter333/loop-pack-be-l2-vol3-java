package com.loopers.domain.metrics;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductMetrics {

    private Long productId;
    private Long likeCount;
    private Long viewCount;
    private Long salesCount;
    private Long salesAmount;
    private LocalDateTime updatedAt;

    public ProductMetrics(Long productId) {
        this.productId = productId;
        this.likeCount = 0L;
        this.viewCount = 0L;
        this.salesCount = 0L;
        this.salesAmount = 0L;
        this.updatedAt = LocalDateTime.now();
    }

    public ProductMetrics(Long productId, Long likeCount, Long viewCount,
                          Long salesCount, Long salesAmount, LocalDateTime updatedAt) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
        this.updatedAt = updatedAt;
    }
}
