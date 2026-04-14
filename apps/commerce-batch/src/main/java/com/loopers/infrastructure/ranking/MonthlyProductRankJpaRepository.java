package com.loopers.infrastructure.ranking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonthlyProductRankJpaRepository extends JpaRepository<MonthlyProductRankEntity, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO mv_product_rank_monthly
            (product_id, year_month, like_count, view_count, sales_count, sales_amount, score, `rank`, created_at, updated_at)
        VALUES
            (:productId, :yearMonth, :likeCount, :viewCount, :salesCount, :salesAmount, :score, :rank, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            like_count = VALUES(like_count),
            view_count = VALUES(view_count),
            sales_count = VALUES(sales_count),
            sales_amount = VALUES(sales_amount),
            score = VALUES(score),
            `rank` = VALUES(`rank`),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsert(
        @Param("productId") Long productId,
        @Param("yearMonth") String yearMonth,
        @Param("likeCount") Long likeCount,
        @Param("viewCount") Long viewCount,
        @Param("salesCount") Long salesCount,
        @Param("salesAmount") Long salesAmount,
        @Param("score") Double score,
        @Param("rank") Integer rank
    );
}
