package com.loopers.infrastructure.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {

    List<BrandEntity> findByDeletedAtIsNull();

    Page<BrandEntity> findByDeletedAtIsNull(Pageable pageable);
}