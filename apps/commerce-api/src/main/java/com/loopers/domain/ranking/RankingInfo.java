package com.loopers.domain.ranking;

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
