package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final AdminValidator adminValidator;

    public ProductInfo getProductInfo(Long productId) {
        Product product = productService.getActiveProduct(productId);
        Brand brand = brandService.getActiveBrand(product.getBrandId());
        Long likeCount = 0L; // TODO: Like 도메인 구현 후 연동
        return ProductInfo.from(product, BrandInfo.from(brand), likeCount);
    }

    public List<ProductInfo> getProductInfos() {
        return productService.getAllActiveProducts().stream()
            .map(product -> {
                Brand brand = brandService.getActiveBrand(product.getBrandId());
                Long likeCount = 0L; // TODO: Like 도메인 구현 후 연동
                return ProductInfo.from(product, BrandInfo.from(brand), likeCount);
            })
            .toList();
    }

    public ProductDetailInfo getProductDetail(String ldap, Long productId) {
        adminValidator.validate(ldap);
        Product product = productService.getProduct(productId);
        return ProductDetailInfo.from(product);
    }

    public ProductDetailInfo createProduct(String ldap, ProductCommand.Create command) {
        adminValidator.validate(ldap);
        Product product = productService.createProduct(
            command.name(), command.brandId(), command.categoryId(), command.basePrice()
        );
        return ProductDetailInfo.from(product);
    }

    public ProductDetailInfo updateProduct(String ldap, Long productId, ProductCommand.Update command) {
        adminValidator.validate(ldap);
        Product product = productService.updateProduct(
            productId, command.name(), command.categoryId(), command.basePrice(),
            command.discount(), command.discountType(), command.status()
        );
        return ProductDetailInfo.from(product);
    }

    public void deleteProduct(String ldap, Long productId) {
        adminValidator.validate(ldap);
        productService.deleteProduct(productId);
    }
}