package com.loopers.interfaces.api.product;

import com.loopers.application.product.ProductAdminDetailInfo;
import com.loopers.application.product.ProductCommand;
import com.loopers.application.product.ProductFacade;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/admin/products")
public class ProductAdminV1Controller implements ProductAdminV1ApiSpec {

    private final ProductFacade productFacade;

    @GetMapping("/{productId}")
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductDetailResponse> getProduct(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long productId
    ) {
        ProductAdminDetailInfo info = productFacade.getProductDetail(ldap, productId);
        ProductAdminV1Dto.ProductDetailResponse response = ProductAdminV1Dto.ProductDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductDetailResponse> createProduct(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @Valid @RequestBody ProductAdminV1Dto.CreateProductRequest request
    ) {
        ProductCommand.Create command = new ProductCommand.Create(
            request.name(), request.brandId(), request.categoryId(), request.basePrice()
        );
        ProductAdminDetailInfo info = productFacade.createProduct(ldap, command);
        ProductAdminV1Dto.ProductDetailResponse response = ProductAdminV1Dto.ProductDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @PutMapping("/{productId}")
    @Override
    public ApiResponse<ProductAdminV1Dto.ProductDetailResponse> updateProduct(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long productId,
        @Valid @RequestBody ProductAdminV1Dto.UpdateProductRequest request
    ) {
        ProductCommand.Update command = new ProductCommand.Update(
            request.name(), request.categoryId(), request.basePrice(),
            request.discount(), request.discountType(), request.status()
        );
        ProductAdminDetailInfo info = productFacade.updateProduct(ldap, productId, command);
        ProductAdminV1Dto.ProductDetailResponse response = ProductAdminV1Dto.ProductDetailResponse.from(info);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{productId}")
    @Override
    public ApiResponse<Object> deleteProduct(
        @RequestHeader("X-Loopers-Ldap") String ldap,
        @PathVariable Long productId
    ) {
        productFacade.deleteProduct(ldap, productId);
        return ApiResponse.success();
    }
}