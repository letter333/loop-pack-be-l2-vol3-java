package com.loopers.infrastructure.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetricsEntity, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, sales_amount, updated_at)
        VALUES (:productId, :delta, 0, 0, 0, NOW())
        ON DUPLICATE KEY UPDATE like_count = like_count + :delta, updated_at = NOW()
        """, nativeQuery = true)
    void incrementLikeCount(@Param("productId") Long productId, @Param("delta") long delta);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, sales_amount, updated_at)
        VALUES (:productId, 0, :delta, 0, 0, NOW())
        ON DUPLICATE KEY UPDATE view_count = view_count + :delta, updated_at = NOW()
        """, nativeQuery = true)
    void incrementViewCount(@Param("productId") Long productId, @Param("delta") long delta);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, view_count, sales_count, sales_amount, updated_at)
        VALUES (:productId, 0, 0, :count, :amount, NOW())
        ON DUPLICATE KEY UPDATE sales_count = sales_count + :count, sales_amount = sales_amount + :amount, updated_at = NOW()
        """, nativeQuery = true)
    void incrementSalesCount(@Param("productId") Long productId, @Param("count") long count, @Param("amount") long amount);
}
