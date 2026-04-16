package com.loopers.infrastructure.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_product_rank_monthly",
    uniqueConstraints = @UniqueConstraint(name = "uk_product_year_month", columnNames = {"product_id", "year_month"}),
    indexes = @Index(name = "idx_year_month_rank", columnList = "year_month, rank")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyProductRankEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "year_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    @Column(name = "sales_count", nullable = false)
    private Long salesCount;

    @Column(name = "sales_amount", nullable = false)
    private Long salesAmount;

    @Column(name = "score", nullable = false)
    private Double score;

    @Column(name = "`rank`", nullable = false)
    private Integer rank;
}
