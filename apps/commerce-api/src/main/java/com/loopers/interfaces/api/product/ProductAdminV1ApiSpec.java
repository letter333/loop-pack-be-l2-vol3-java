package com.loopers.interfaces.api.product;

import com.loopers.interfaces.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "Product Admin V1 API", description = "상품 관리자 API 입니다.")
public interface ProductAdminV1ApiSpec {

    @Operation(
        summary = "상품 목록 조회 (Admin)",
        description = "관리자용 상품 목록을 조회합니다. 삭제된 상품도 포함됩니다."
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    ApiResponse<Page<ProductAdminV1Dto.ProductDetailResponse>> getProducts(String ldap, Pageable pageable);

    @Operation(
        summary = "상품 상세 조회 (Admin)",
        description = "관리자용 상품 상세 정보를 조회합니다."
    )
    ApiResponse<ProductAdminV1Dto.ProductDetailResponse> getProduct(String ldap, Long productId);

    @Operation(
        summary = "상품 등록",
        description = "새로운 상품을 등록합니다."
    )
    ApiResponse<ProductAdminV1Dto.ProductDetailResponse> createProduct(
        String ldap, ProductAdminV1Dto.CreateProductRequest request
    );

    @Operation(
        summary = "상품 수정",
        description = "상품 정보를 수정합니다."
    )
    ApiResponse<ProductAdminV1Dto.ProductDetailResponse> updateProduct(
        String ldap, Long productId, ProductAdminV1Dto.UpdateProductRequest request
    );

    @Operation(
        summary = "상품 삭제",
        description = "상품을 삭제합니다. (Soft Delete)"
    )
    ApiResponse<Object> deleteProduct(String ldap, Long productId);
}