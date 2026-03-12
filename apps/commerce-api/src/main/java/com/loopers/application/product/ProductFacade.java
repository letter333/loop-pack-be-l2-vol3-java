package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.category.CategoryService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSortType;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final AdminValidator adminValidator;

    @Transactional(readOnly = true)
    public ProductDetailInfo getProduct(Long productId) {
        Product product = productService.getActiveProduct(productId);
        Brand brand = brandService.getActiveBrand(product.getBrandId());
        return ProductDetailInfo.from(product, BrandInfo.from(brand), product.getLikeCount());
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(Long categoryId, Long brandId, String keyword, ProductSortType sort, Pageable pageable) {
        Page<Product> products = productService.getProducts(categoryId, brandId, keyword, sort, pageable);

        // 1. 모든 brandId 수집 (중복 제거)
        List<Long> brandIds = products.getContent().stream()
            .map(Product::getBrandId)
            .distinct()
            .toList();

        // 2. 한 번의 쿼리로 모든 브랜드 조회
        Map<Long, Brand> brandMap = brandService.getActiveBrandsByIds(brandIds);

        // 3. 매핑
        return products.map(product -> {
            Brand brand = brandMap.get(product.getBrandId());
            BrandInfo brandInfo = brand != null ? BrandInfo.from(brand) : null;
            return ProductInfo.from(product, brandInfo, product.getLikeCount());
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
        brandService.validateBrand(command.brandId());
        categoryService.validateCategory(command.categoryId());
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

    @Transactional(readOnly = true)
    public Page<ProductAdminDetailInfo> getProductsForAdmin(String ldap, Pageable pageable) {
        adminValidator.validate(ldap);
        return productService.getProductsForAdmin(pageable)
            .map(ProductAdminDetailInfo::from);
    }
}