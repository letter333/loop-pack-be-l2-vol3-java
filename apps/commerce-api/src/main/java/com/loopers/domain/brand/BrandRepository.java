package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface BrandRepository {

    Optional<Brand> findById(Long id);

    Page<Brand> findAllActive(Pageable pageable);

    Brand save(Brand brand);

    Brand update(Long id, Brand brand);

    void delete(Long id);

    boolean existsById(Long id);
}