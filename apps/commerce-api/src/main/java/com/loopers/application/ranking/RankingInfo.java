package com.loopers.application.ranking;

public record RankingInfo(
    Long productId,
    Integer rank,
    Double score,
    Long likeCount,
    Long viewCount,
    Long salesCount,
    Long salesAmount
) {
}
