package com.loopers.application.product;

import com.loopers.application.ranking.RankingDetailInfo;

public record ProductDetailWithRankingInfo(
    ProductDetailInfo productDetail,
    RankingDetailInfo ranking
) {
}
