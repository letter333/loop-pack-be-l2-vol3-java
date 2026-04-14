package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;

public class RankingV1Dto {

    public record RankingResponse(
        Long productId,
        Integer rank,
        Double score,
        Long likeCount,
        Long viewCount,
        Long salesCount,
        Long salesAmount
    ) {
        public static RankingResponse from(RankingInfo info) {
            return new RankingResponse(
                info.productId(),
                info.rank(),
                info.score(),
                info.likeCount(),
                info.viewCount(),
                info.salesCount(),
                info.salesAmount()
            );
        }
    }
}
