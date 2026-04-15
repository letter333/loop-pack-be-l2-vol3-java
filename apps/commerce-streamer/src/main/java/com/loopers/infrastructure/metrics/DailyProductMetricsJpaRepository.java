package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyProductMetricsJpaRepository extends JpaRepository<DailyProductMetricsEntity, DailyProductMetricsId> {

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics_daily (product_id, metric_date, like_count, view_count, sales_count, sales_amount, updated_at)
        VALUES (:productId, CURDATE(), :delta, 0, 0, 0, NOW())
        ON DUPLICATE KEY UPDATE like_count = like_count + :delta, updated_at = NOW()
        """, nativeQuery = true)
    void incrementLikeCount(@Param("productId") Long productId, @Param("delta") long delta);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics_daily (product_id, metric_date, like_count, view_count, sales_count, sales_amount, updated_at)
        VALUES (:productId, CURDATE(), 0, :delta, 0, 0, NOW())
        ON DUPLICATE KEY UPDATE view_count = view_count + :delta, updated_at = NOW()
        """, nativeQuery = true)
    void incrementViewCount(@Param("productId") Long productId, @Param("delta") long delta);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics_daily (product_id, metric_date, like_count, view_count, sales_count, sales_amount, updated_at)
        VALUES (:productId, CURDATE(), 0, 0, :count, :amount, NOW())
        ON DUPLICATE KEY UPDATE sales_count = sales_count + :count, sales_amount = sales_amount + :amount, updated_at = NOW()
        """, nativeQuery = true)
    void incrementSalesCount(@Param("productId") Long productId, @Param("count") long count, @Param("amount") long amount);
}
