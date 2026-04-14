package com.loopers.application.ranking;

import com.loopers.domain.ranking.ProductRankingRepository;
import com.loopers.domain.ranking.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RankingFacade {

    private final ProductRankingRepository productRankingRepository;

    public RankingDetailInfo getProductRanking(Long productId) {
        String dateKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return productRankingRepository.getProductDailyRanking(dateKey, productId)
            .orElse(null);
    }

    public List<RankingInfo> getRankings(RankingPeriod period, String date, int page, int size) {
        int cursor = (page - 1) * size;

        return switch (period) {
            case DAILY -> {
                String dateKey = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                yield productRankingRepository.getDailyRankings(dateKey, cursor, size);
            }
            case WEEKLY -> {
                String yearWeek = toYearWeek(date);
                yield productRankingRepository.getWeeklyRankings(yearWeek, cursor, size);
            }
            case MONTHLY -> {
                String yearMonth = toYearMonth(date);
                yield productRankingRepository.getMonthlyRankings(yearMonth, cursor, size);
            }
        };
    }

    private String toYearWeek(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return String.format("%d-W%02d", year, weekNumber);
    }

    private String toYearMonth(String dateStr) {
        LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}
