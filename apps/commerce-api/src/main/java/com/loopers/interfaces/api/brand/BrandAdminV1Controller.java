package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandCommand;
import com.loopers.application.brand.BrandDetailInfo;
import com.loopers.application.brand.BrandFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/brands")
public class BrandAdminV1Controller implements BrandAdminV1ApiSpec {

    private final BrandFacade brandFacade;

    @GetMapping
    @Override
    public ApiResponse<Page<BrandAdminV1Dto.BrandDetailResponse>> getBrands(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        Pageable pageable
    ) {
        Page<BrandDetailInfo> infos = brandFacade.getBrandDetails(ldap, pageable);
        Page<BrandAdminV1Dto.BrandDetailResponse> response = infos.map(BrandAdminV1Dto.BrandDetailResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandDetailResponse> getBrand(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long brandId
    ) {
        BrandDetailInfo info = brandFacade.getBrandDetail(ldap, brandId);
        BrandAdminV1Dto.BrandDetailResponse response = BrandAdminV1Dto.BrandDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandDetailResponse> createBrand(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @Valid @RequestBody BrandAdminV1Dto.CreateBrandRequest request
    ) {
        BrandCommand.Create command = new BrandCommand.Create(request.name(), request.description(), request.logoImageUrl());
        BrandDetailInfo info = brandFacade.createBrand(ldap, command);
        BrandAdminV1Dto.BrandDetailResponse response = BrandAdminV1Dto.BrandDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PutMapping("/{brandId}")
    @Override
    public ApiResponse<BrandAdminV1Dto.BrandDetailResponse> updateBrand(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long brandId,
        @Valid @RequestBody BrandAdminV1Dto.UpdateBrandRequest request
    ) {
        BrandCommand.Update command = new BrandCommand.Update(request.name(), request.description(), request.logoImageUrl());
        BrandDetailInfo info = brandFacade.updateBrand(ldap, brandId, command);
        BrandAdminV1Dto.BrandDetailResponse response = BrandAdminV1Dto.BrandDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{brandId}")
    @Override
    public ApiResponse<Object> deleteBrand(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long brandId
    ) {
        brandFacade.deleteBrand(ldap, brandId);
        return ApiResponse.success();
    }
}