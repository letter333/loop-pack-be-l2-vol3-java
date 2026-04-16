package com.loopers.application.product;

import com.loopers.domain.ranking.RankingDetailInfo;

public record ProductDetailWithRankingInfo(
    ProductDetailInfo productDetail,
    RankingDetailInfo ranking
) {
}
