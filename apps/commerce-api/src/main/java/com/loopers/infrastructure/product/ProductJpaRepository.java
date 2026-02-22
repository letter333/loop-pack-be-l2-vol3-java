package com.loopers.infrastructure.product;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, ProductJpaRepositoryCustom {

    Optional<ProductEntity> findByIdAndDeletedAtIsNull(Long id);

    List<ProductEntity> findAllByDeletedAtIsNull();

    List<ProductEntity> findAllByCategoryIdAndDeletedAtIsNull(Long categoryId);

    boolean existsByIdAndDeletedAtIsNull(Long id);
}