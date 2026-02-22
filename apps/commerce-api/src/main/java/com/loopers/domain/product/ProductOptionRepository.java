package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {

    Optional<ProductOption> findById(Long id);

    List<ProductOption> findAllByProductId(Long productId);

    ProductOption save(ProductOption productOption);

    void delete(Long id);
}