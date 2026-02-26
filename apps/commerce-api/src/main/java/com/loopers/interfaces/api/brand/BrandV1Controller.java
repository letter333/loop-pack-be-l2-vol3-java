package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandFacade;
import com.loopers.application.brand.BrandInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/brands")
public class BrandV1Controller implements BrandV1ApiSpec {

    private final BrandFacade brandFacade;

    @GetMapping
    @Override
    public ApiResponse<Page<BrandV1Dto.BrandResponse>> getBrands(Pageable pageable) {
        Page<BrandInfo> infos = brandFacade.getBrandInfos(pageable);
        Page<BrandV1Dto.BrandResponse> response = infos.map(BrandV1Dto.BrandResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/{brandId}")
    @Override
    public ApiResponse<BrandV1Dto.BrandResponse> getBrand(@PathVariable Long brandId) {
        BrandInfo info = brandFacade.getBrandInfo(brandId);
        BrandV1Dto.BrandResponse response = BrandV1Dto.BrandResponse.from(info);
        return ApiResponse.success(response);
    }
}