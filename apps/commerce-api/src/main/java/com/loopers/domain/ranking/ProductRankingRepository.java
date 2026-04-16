package com.loopers.domain.ranking;

import com.loopers.application.ranking.RankingDetailInfo;
import com.loopers.application.ranking.RankingInfo;

import java.util.List;
import java.util.Optional;

public interface ProductRankingRepository {

    List<RankingInfo> getDailyRankings(String dateKey, int cursor, int size);

    Optional<RankingDetailInfo> getProductDailyRanking(String dateKey, Long productId);

    List<RankingInfo> getWeeklyRankings(String yearWeek, int cursor, int size);

    List<RankingInfo> getMonthlyRankings(String yearMonth, int cursor, int size);
}
