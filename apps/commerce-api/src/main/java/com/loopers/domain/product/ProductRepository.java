package com.loopers.domain.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    List<Product> findAllActive();

    List<Product> findAllActiveByCategoryId(Long categoryId);

    Page<Product> findProducts(Long categoryId, String keyword, ProductSortType sort, Pageable pageable);

    Product save(Product product);

    void delete(Long id);

    boolean existsById(Long id);
}