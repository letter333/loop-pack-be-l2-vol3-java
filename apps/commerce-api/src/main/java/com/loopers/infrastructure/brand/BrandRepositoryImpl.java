package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BrandRepositoryImpl implements BrandRepository {

    private final BrandJpaRepository brandJpaRepository;

    @Override
    public Optional<Brand> findById(Long id) {
        return brandJpaRepository.findById(id)
            .map(BrandEntity::toDomain);
    }

    @Override
    public List<Brand> findAllActive() {
        return brandJpaRepository.findByDeletedAtIsNull()
            .stream()
            .map(BrandEntity::toDomain)
            .toList();
    }

    @Override
    public Page<Brand> findAllActive(Pageable pageable) {
        return brandJpaRepository.findByDeletedAtIsNull(pageable)
            .map(BrandEntity::toDomain);
    }

    @Override
    public Brand save(Brand brand) {
        BrandEntity entity;
        if (brand.getId() != null) {
            entity = brandJpaRepository.findById(brand.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
            entity.update(brand.getName(), brand.getDescription(), brand.getLogoImageUrl());
            entity.updateLikeCount(brand.getLikeCount());
        } else {
            entity = BrandEntity.from(brand);
        }
        BrandEntity saved = brandJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Long id) {
        brandJpaRepository.deleteById(id);
    }

    @Override
    public Brand update(Long id, Brand brand) {
        BrandEntity entity = brandJpaRepository.findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
        entity.update(brand.getName(), brand.getDescription(), brand.getLogoImageUrl());
        return entity.toDomain();
    }

    @Override
    public boolean existsById(Long id) {
        return brandJpaRepository.existsById(id);
    }
}