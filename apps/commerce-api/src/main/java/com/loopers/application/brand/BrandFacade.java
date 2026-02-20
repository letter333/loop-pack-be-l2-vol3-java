package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrandFacade {

    private final BrandService brandService;
    private final AdminValidator adminValidator;

    public BrandInfo getBrandInfo(Long brandId) {
        Brand brand = brandService.getActiveBrand(brandId);
        return BrandInfo.from(brand);
    }

    public Page<BrandInfo> getBrandInfos(Pageable pageable) {
        return brandService.getBrands(pageable)
            .map(BrandInfo::from);
    }

    public BrandDetailInfo getBrandDetail(String ldap, Long brandId) {
        adminValidator.validate(ldap);
        Brand brand = brandService.getBrand(brandId);
        return BrandDetailInfo.from(brand);
    }

    public Page<BrandDetailInfo> getBrandDetails(String ldap, Pageable pageable) {
        adminValidator.validate(ldap);
        return brandService.getBrands(pageable)
            .map(BrandDetailInfo::from);
    }

    public BrandDetailInfo createBrand(String ldap, String name, String description, String logoImageUrl) {
        adminValidator.validate(ldap);
        Brand brand = brandService.createBrand(name, description, logoImageUrl);
        return BrandDetailInfo.from(brand);
    }

    public BrandDetailInfo updateBrand(String ldap, Long brandId, String name, String description, String logoImageUrl) {
        adminValidator.validate(ldap);
        Brand brand = brandService.updateBrand(brandId, name, description, logoImageUrl);
        return BrandDetailInfo.from(brand);
    }

    public void deleteBrand(String ldap, Long brandId) {
        adminValidator.validate(ldap);
        brandService.deleteBrand(brandId);
    }
}