package com.loopers.infrastructure.ranking;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyProductRankJpaRepository extends JpaRepository<MonthlyProductRankEntity, Long> {

    List<MonthlyProductRankEntity> findByYearMonthOrderByRankAsc(String yearMonth, Pageable pageable);
}
