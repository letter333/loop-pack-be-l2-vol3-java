package com.loopers.infrastructure.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {

    Page<BrandEntity> findByDeletedAtIsNull(Pageable pageable);
}