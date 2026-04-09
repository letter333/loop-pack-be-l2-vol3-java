package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductDetailWithRankingInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.domain.product.ProductSortType;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductV1Controller implements ProductV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping
    @Override
    public ApiResponse<Page<ProductV1Dto.ProductResponse>> getProducts(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Long brandId,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "LATEST") ProductSortType sort,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductInfo> infos = productFacade.getProducts(categoryId, brandId, keyword, sort, pageable);
        Page<ProductV1Dto.ProductResponse> response = infos.map(ProductV1Dto.ProductResponse::from);
        return ApiResponse.success(response);
    }

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductV1Dto.ProductDetailResponse> getProduct(@PathVariable Long productId) {
        ProductDetailWithRankingInfo info = productFacade.getProduct(productId);
        ProductV1Dto.ProductDetailResponse response = ProductV1Dto.ProductDetailResponse.from(info);
        return ApiResponse.success(response);
    }
}