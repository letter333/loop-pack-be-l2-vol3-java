package com.loopers.domain.ranking;

import lombok.Getter;

@Getter
public class ProductRankMv {

    private final Long productId;
    private final String periodKey;
    private final Long likeCount;
    private final Long viewCount;
    private final Long salesCount;
    private final Long salesAmount;
    private final Double score;
    private final Integer rank;

    public ProductRankMv(Long productId, String periodKey, Long likeCount, Long viewCount,
                         Long salesCount, Long salesAmount, Double score, Integer rank) {
        this.productId = productId;
        this.periodKey = periodKey;
        this.likeCount = likeCount;
        this.viewCount = viewCount;
        this.salesCount = salesCount;
        this.salesAmount = salesAmount;
        this.score = score;
        this.rank = rank;
    }

    public static ProductRankMv from(ProductMetricsAggregation aggregation, String periodKey) {
        return new ProductRankMv(
            aggregation.getProductId(),
            periodKey,
            aggregation.getLikeCount(),
            aggregation.getViewCount(),
            aggregation.getSalesCount(),
            aggregation.getSalesAmount(),
            aggregation.getScore(),
            aggregation.getRank()
        );
    }
}
