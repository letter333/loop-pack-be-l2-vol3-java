package com.loopers.domain.product;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository {

    Optional<ProductImage> findById(Long id);

    List<ProductImage> findAllByProductId(Long productId);

    ProductImage save(ProductImage productImage);

    void delete(Long id);

    void deleteAllByProductId(Long productId);
}