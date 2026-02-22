package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductSortType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductJpaRepositoryCustom {

    Page<ProductEntity> findProducts(Long categoryId, String keyword, ProductSortType sort, Pageable pageable);
}
