package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Optional<Product> findById(Long id);

    List<Product> findAllActive();

    List<Product> findAllActiveByCategoryId(Long categoryId);

    Product save(Product product);

    void delete(Long id);

    boolean existsById(Long id);
}