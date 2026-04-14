package com.loopers.infrastructure.ranking;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyProductRankJpaRepository extends JpaRepository<WeeklyProductRankEntity, Long> {

    List<WeeklyProductRankEntity> findByYearWeekAndRankGreaterThanOrderByRankAsc(
        String yearWeek, Integer rankCursor, Pageable pageable
    );
}
