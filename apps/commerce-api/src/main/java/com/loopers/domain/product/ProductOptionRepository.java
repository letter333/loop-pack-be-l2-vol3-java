package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductOptionRepository {

    Optional<ProductOption> findById(Long id);

    List<ProductOption> findAllByProductId(Long productId);

    ProductOption save(ProductOption productOption);

    void decreaseStock(Long id, int quantity);

    void increaseStock(Long id, int quantity);

    void delete(Long id);
}