package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;

    @Transactional(readOnly = true)
    public Brand getBrand(Long brandId) {
        return brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Brand getActiveBrand(Long brandId) {
        Brand brand = getBrand(brandId);
        if (brand.isDeleted()) {
            throw new CoreException(ErrorType.NOT_FOUND, "삭제된 브랜드입니다.");
        }
        return brand;
    }

    @Transactional(readOnly = true)
    public Page<Brand> getBrands(Pageable pageable) {
        return brandRepository.findAllActive(pageable);
    }

    @Transactional
    public Brand createBrand(String name, String description, String logoImageUrl) {
        Brand brand = new Brand(name, description, logoImageUrl);
        return brandRepository.save(brand);
    }

    @Transactional
    public Brand updateBrand(Long brandId, String name, String description, String logoImageUrl) {
        Brand brand = new Brand(name, description, logoImageUrl);
        return brandRepository.update(brandId, brand);
    }

    @Transactional
    public void deleteBrand(Long brandId) {
        getBrand(brandId);  // 존재 확인
        brandRepository.delete(brandId);
    }

    @Transactional(readOnly = true)
    public Brand validateBrand(Long brandId) {
        return getActiveBrand(brandId);
    }
}