package com.loopers.infrastructure.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageJpaRepository extends JpaRepository<ProductImageEntity, Long> {

    List<ProductImageEntity> findByProductId(Long productId);

    void deleteByProductId(Long productId);
}