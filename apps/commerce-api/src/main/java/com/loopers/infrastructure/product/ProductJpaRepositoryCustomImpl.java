package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductSortType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ProductJpaRepositoryCustomImpl implements ProductJpaRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<ProductEntity> findProducts(Long categoryId, Long brandId, String keyword, ProductSortType sort, Pageable pageable) {
        StringBuilder sql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("SELECT * FROM products WHERE deleted_at IS NULL");
        countSql.append("SELECT COUNT(*) FROM products WHERE deleted_at IS NULL");

        if (categoryId != null) {
            sql.append(" AND category_id = :categoryId");
            countSql.append(" AND category_id = :categoryId");
            params.put("categoryId", categoryId);
        }

        if (brandId != null) {
            sql.append(" AND brand_id = :brandId");
            countSql.append(" AND brand_id = :brandId");
            params.put("brandId", brandId);
        }

        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)");
            countSql.append(" AND MATCH(name) AGAINST(:keyword IN BOOLEAN MODE)");
            params.put("keyword", "*" + keyword + "*");
        }

        sql.append(getSortClause(sort));
        sql.append(" LIMIT :limit OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString(), ProductEntity.class);
        Query countQuery = entityManager.createNativeQuery(countSql.toString());

        params.forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
        });
        query.setParameter("limit", pageable.getPageSize());
        query.setParameter("offset", pageable.getOffset());

        List<ProductEntity> content = query.getResultList();
        Long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, pageable, total);
    }

    private String getSortClause(ProductSortType sort) {
        if (sort == null) {
            return " ORDER BY created_at DESC";
        }
        return switch (sort) {
            case PRICE_ASC -> " ORDER BY base_price ASC";
            case LIKES_DESC -> " ORDER BY like_count DESC, created_at DESC";
            default -> " ORDER BY created_at DESC";
        };
    }
}
