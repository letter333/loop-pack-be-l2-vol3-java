package com.loopers.domain.brand;

import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandService {

    private final BrandRepository brandRepository;
    private final ProductService productService;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Brand getBrand(Long brandId) {
        return brandRepository.findById(brandId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Brand getActiveBrand(Long brandId) {
        Brand brand = getBrand(brandId);
        if (brand.isDeleted()) {
            throw new CoreException(ErrorType.NOT_FOUND, "삭제된 브랜드입니다.");
        }
        return brand;
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Page<Brand> getBrands(Pageable pageable) {
        return brandRepository.findAllActive(pageable);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Brand createBrand(String name, String description, String logoImageUrl) {
        Brand brand = new Brand(name, description, logoImageUrl);
        return brandRepository.save(brand);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Brand updateBrand(Long brandId, String name, String description, String logoImageUrl) {
        Brand brand = new Brand(name, description, logoImageUrl);
        return brandRepository.update(brandId, brand);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteBrand(Long brandId) {
        getBrand(brandId);  // 존재 확인
        productService.deleteProductsByBrandId(brandId);
        brandRepository.delete(brandId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Brand validateBrand(Long brandId) {
        return getActiveBrand(brandId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Map<Long, Brand> getActiveBrandsByIds(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return Map.of();
        }
        List<Brand> brands = brandRepository.findAllActiveByIds(brandIds);
        return brands.stream()
            .collect(Collectors.toMap(Brand::getId, brand -> brand));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long increaseLikeCount(Long brandId) {
        return brandRepository.increaseLikeCount(brandId);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Long decreaseLikeCount(Long brandId) {
        return brandRepository.decreaseLikeCount(brandId);
    }
}