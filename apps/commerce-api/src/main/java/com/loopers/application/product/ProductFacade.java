package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSortType;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final AdminValidator adminValidator;

    @Transactional(readOnly = true)
    public ProductDetailInfo getProduct(Long productId) {
        Product product = productService.getActiveProduct(productId);
        Brand brand = brandService.getActiveBrand(product.getBrandId());
        Long likeCount = 0L; // TODO: Like 도메인 구현 후 연동
        return ProductDetailInfo.from(product, BrandInfo.from(brand), likeCount);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(Long categoryId, String keyword, ProductSortType sort, Pageable pageable) {
        Page<Product> products = productService.getProducts(categoryId, keyword, sort, pageable);
        return products.map(product -> {
            Brand brand = brandService.getActiveBrand(product.getBrandId());
            Long likeCount = 0L; // TODO: Like 도메인 구현 후 연동
            return ProductInfo.from(product, BrandInfo.from(brand), likeCount);
        });
    }

    @Transactional(readOnly = true)
    public ProductAdminDetailInfo getProductDetail(String ldap, Long productId) {
        adminValidator.validate(ldap);
        Product product = productService.getProduct(productId);
        return ProductAdminDetailInfo.from(product);
    }

    @Transactional
    public ProductAdminDetailInfo createProduct(String ldap, ProductCommand.Create command) {
        adminValidator.validate(ldap);
        Product product = productService.createProduct(
            command.name(), command.brandId(), command.categoryId(), command.basePrice()
        );
        return ProductAdminDetailInfo.from(product);
    }

    @Transactional
    public ProductAdminDetailInfo updateProduct(String ldap, Long productId, ProductCommand.Update command) {
        adminValidator.validate(ldap);
        Product product = productService.updateProduct(
            productId, command.name(), command.categoryId(), command.basePrice(),
            command.discount(), command.discountType(), command.status()
        );
        return ProductAdminDetailInfo.from(product);
    }

    @Transactional
    public void deleteProduct(String ldap, Long productId) {
        adminValidator.validate(ldap);
        productService.deleteProduct(productId);
    }
}