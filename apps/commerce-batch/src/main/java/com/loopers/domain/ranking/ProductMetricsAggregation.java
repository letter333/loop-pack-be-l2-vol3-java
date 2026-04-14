package com.loopers.domain.ranking;

import lombok.Getter;

@Getter
public class ProductMetricsAggregation {

    private final Long productId;
    private final Long likeCount;
    private final Long viewCount;
    private final Long salesCount;
    private final Long salesAmount;
    private final Double score;

    public ProductMetricsAggregation(Long productId, Long likeCount, Long viewCount,
                                     Long salesCount, Long salesAmount) {
        this.productId = productId;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
        this.score = viewCount * 0.1 + likeCount * 0.2 + salesAmount * 0.6;
    }
}
