package com.loopers.infrastructure.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryJpaRepository extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findByDeletedAtIsNull();

    List<CategoryEntity> findByPathStartingWithAndDeletedAtIsNull(String pathPrefix);
}