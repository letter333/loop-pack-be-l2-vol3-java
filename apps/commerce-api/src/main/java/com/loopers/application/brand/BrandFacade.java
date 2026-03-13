package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class BrandFacade {

    private final BrandService brandService;
    private final AdminValidator adminValidator;

    @Transactional(readOnly = true)
    public BrandInfo getBrandInfo(Long brandId) {
        Brand brand = brandService.getActiveBrand(brandId);
        return BrandInfo.from(brand);
    }

    @Transactional(readOnly = true)
    public Page<BrandInfo> getBrandInfos(Pageable pageable) {
        return brandService.getBrands(pageable)
            .map(BrandInfo::from);
    }

    @Transactional(readOnly = true)
    public BrandDetailInfo getBrandDetail(String ldap, Long brandId) {
        adminValidator.validate(ldap);
        Brand brand = brandService.getBrand(brandId);
        return BrandDetailInfo.from(brand);
    }

    @Transactional(readOnly = true)
    public Page<BrandDetailInfo> getBrandDetails(String ldap, Pageable pageable) {
        adminValidator.validate(ldap);
        return brandService.getBrands(pageable)
            .map(BrandDetailInfo::from);
    }

    @Transactional
    public BrandDetailInfo createBrand(String ldap, BrandCommand.Create command) {
        adminValidator.validate(ldap);
        Brand brand = brandService.createBrand(command.name(), command.description(), command.logoImageUrl());
        return BrandDetailInfo.from(brand);
    }

    @Transactional
    public BrandDetailInfo updateBrand(String ldap, Long brandId, BrandCommand.Update command) {
        adminValidator.validate(ldap);
        Brand brand = brandService.updateBrand(brandId, command.name(), command.description(), command.logoImageUrl());
        return BrandDetailInfo.from(brand);
    }

    @Transactional
    public void deleteBrand(String ldap, Long brandId) {
        adminValidator.validate(ldap);
        brandService.deleteBrand(brandId);
    }
}