package com.loopers.infrastructure.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductOptionJpaRepository extends JpaRepository<ProductOptionEntity, Long> {

    List<ProductOptionEntity> findByProductIdAndDeletedAtIsNull(Long productId);
}