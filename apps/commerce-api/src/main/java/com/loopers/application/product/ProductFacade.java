package com.loopers.application.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.domain.ranking.RankingDetailInfo;
import com.loopers.application.ranking.RankingFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.category.CategoryService;
import com.loopers.domain.product.Product;
import com.loopers.application.event.ProductViewedEvent;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductSortType;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductFacade {

    private final ProductService productService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final AdminValidator adminValidator;
    private final ProductDetailCacheRepository productDetailCacheRepository;
    private final ProductListCacheRepository productListCacheRepository;
    private final RankingFacade rankingFacade;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ProductDetailWithRankingInfo getProduct(Long productId) {
        applicationEventPublisher.publishEvent(new ProductViewedEvent(productId));
        ProductDetailInfo productDetail = productDetailCacheRepository.get(productId)
            .orElseGet(() -> {
                Product product = productService.getActiveProduct(productId);
                Brand brand = brandService.getActiveBrand(product.getBrandId());
                ProductDetailInfo info = ProductDetailInfo.from(product, BrandInfo.from(brand), product.getLikeCount());
                productDetailCacheRepository.put(productId, info);
                return info;
            });
        RankingDetailInfo ranking = rankingFacade.getProductRanking(productId);
        return new ProductDetailWithRankingInfo(productDetail, ranking);
    }

    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(Long categoryId, Long brandId, String keyword, ProductSortType sort, Pageable pageable) {
        // 키워드 검색은 캐시 우회
        boolean useCache = keyword == null || keyword.isBlank();
        String cacheKey = null;

        if (useCache) {
            cacheKey = productListCacheRepository.generateKey(
                    categoryId, brandId, sort, pageable.getPageNumber(), pageable.getPageSize());
            Optional<Page<ProductInfo>> cached = productListCacheRepository.get(cacheKey);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        Page<Product> products = productService.getProducts(categoryId, brandId, keyword, sort, pageable);

        // 1. 모든 brandId 수집 (중복 제거)
        List<Long> brandIds = products.getContent().stream()
            .map(Product::getBrandId)
            .distinct()
            .toList();

        // 2. 한 번의 쿼리로 모든 브랜드 조회
        Map<Long, Brand> brandMap = brandService.getActiveBrandsByIds(brandIds);

        // 3. 매핑
        Page<ProductInfo> result = products.map(product -> {
            Brand brand = brandMap.get(product.getBrandId());
            BrandInfo brandInfo = brand != null ? BrandInfo.from(brand) : null;
            return ProductInfo.from(product, brandInfo, product.getLikeCount());
        });

        // 키워드 없는 경우만 캐시 저장
        if (useCache) {
            productListCacheRepository.put(cacheKey, result);
        }

        return result;
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
        applicationEventPublisher.publishEvent(ProductCacheEvictEvent.listOnly());
        return ProductAdminDetailInfo.from(product);
    }

    @Transactional
    public ProductAdminDetailInfo updateProduct(String ldap, Long productId, ProductCommand.Update command) {
        adminValidator.validate(ldap);
        Product product = productService.updateProduct(
            productId, command.name(), command.categoryId(), command.basePrice(),
            command.discount(), command.discountType(), command.status()
        );
        applicationEventPublisher.publishEvent(ProductCacheEvictEvent.of(productId));
        return ProductAdminDetailInfo.from(product);
    }

    @Transactional
    public void deleteProduct(String ldap, Long productId) {
        adminValidator.validate(ldap);
        productService.deleteProduct(productId);
        applicationEventPublisher.publishEvent(ProductCacheEvictEvent.of(productId));
    }

    @Transactional(readOnly = true)
    public Page<ProductAdminDetailInfo> getProductsForAdmin(String ldap, Pageable pageable) {
        adminValidator.validate(ldap);
        return productService.getProductsForAdmin(pageable)
            .map(ProductAdminDetailInfo::from);
    }
}