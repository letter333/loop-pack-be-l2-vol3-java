package com.loopers.infrastructure.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductListCacheRepository;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("ProductListCacheRepositoryImpl 통합 테스트")
class ProductListCacheRepositoryImplTest {

    @Autowired
    private ProductListCacheRepository productListCacheRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
    }

    @Test
    @DisplayName("캐시 미스 시 Optional.empty()를 반환한다")
    void returnsEmpty_whenCacheMiss() {
        // Act
        String cacheKey = productListCacheRepository.generateKey(null, null, ProductSortType.LATEST, 0, 20);
        Optional<Page<ProductInfo>> result = productListCacheRepository.get(cacheKey);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("캐시에 저장 후 정상적으로 조회한다")
    void returnsPage_afterPut() {
        // Arrange
        Page<ProductInfo> page = createProductInfoPage();
        String cacheKey = productListCacheRepository.generateKey(1L, 2L, ProductSortType.LATEST, 0, 20);

        // Act
        productListCacheRepository.put(cacheKey, page);
        Optional<Page<ProductInfo>> result = productListCacheRepository.get(cacheKey);

        // Assert
        assertThat(result).isPresent();
        Page<ProductInfo> cached = result.get();
        assertAll(
            () -> assertThat(cached.getContent()).hasSize(2),
            () -> assertThat(cached.getTotalElements()).isEqualTo(2),
            () -> assertThat(cached.getNumber()).isEqualTo(0),
            () -> assertThat(cached.getSize()).isEqualTo(20)
        );

        ProductInfo first = cached.getContent().get(0);
        assertAll(
            () -> assertThat(first.id()).isEqualTo(1L),
            () -> assertThat(first.name()).isEqualTo("아이폰 15"),
            () -> assertThat(first.productCode()).isEqualTo("PROD-001"),
            () -> assertThat(first.basePrice()).isEqualTo(1500000L),
            () -> assertThat(first.discountedPrice()).isEqualTo(1400000L),
            () -> assertThat(first.status()).isEqualTo(ProductStatus.SALE),
            () -> assertThat(first.discount()).isEqualTo(100000L),
            () -> assertThat(first.discountType()).isEqualTo(DiscountType.PRICE),
            () -> assertThat(first.brand()).isNotNull(),
            () -> assertThat(first.brand().name()).isEqualTo("Apple"),
            () -> assertThat(first.likeCount()).isEqualTo(10L)
        );
    }

    @Test
    @DisplayName("캐시 삭제 후 Optional.empty()를 반환한다")
    void returnsEmpty_afterEvict() {
        // Arrange
        Page<ProductInfo> page = createProductInfoPage();
        String cacheKey = productListCacheRepository.generateKey(1L, 2L, ProductSortType.LATEST, 0, 20);
        productListCacheRepository.put(cacheKey, page);

        // Act
        productListCacheRepository.evict(cacheKey);
        Optional<Page<ProductInfo>> result = productListCacheRepository.get(cacheKey);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("전체 목록 캐시 삭제(evictAll) 후 모든 키가 삭제된다")
    void removesAllKeys_afterEvictAll() {
        // Arrange
        Page<ProductInfo> page = createProductInfoPage();
        String key1 = productListCacheRepository.generateKey(1L, null, ProductSortType.LATEST, 0, 20);
        String key2 = productListCacheRepository.generateKey(null, 2L, ProductSortType.PRICE_ASC, 1, 10);
        productListCacheRepository.put(key1, page);
        productListCacheRepository.put(key2, page);

        // Act
        productListCacheRepository.evictAll();

        // Assert
        assertThat(productListCacheRepository.get(key1)).isEmpty();
        assertThat(productListCacheRepository.get(key2)).isEmpty();

        Set<String> remainingKeys = redisTemplate.keys("product:list:*");
        assertThat(remainingKeys).isEmpty();
    }

    @Test
    @DisplayName("캐시 저장 시 TTL이 5분으로 설정된다")
    void setsTtlTo5Minutes() {
        // Arrange
        Page<ProductInfo> page = createProductInfoPage();
        String cacheKey = productListCacheRepository.generateKey(null, null, ProductSortType.LATEST, 0, 20);

        // Act
        productListCacheRepository.put(cacheKey, page);

        // Assert
        Long ttl = redisTemplate.getExpire(cacheKey, TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(290L, 300L);
    }

    @Test
    @DisplayName("캐시 키가 categoryId, brandId, sort, page, size 조합으로 생성된다")
    void generatesCacheKey_withCorrectFormat() {
        // Act & Assert
        assertAll(
            () -> assertThat(productListCacheRepository.generateKey(1L, 2L, ProductSortType.LATEST, 0, 20))
                .isEqualTo("product:list:c1:b2:sLATEST:p0:sz20"),
            () -> assertThat(productListCacheRepository.generateKey(null, null, ProductSortType.PRICE_ASC, 1, 10))
                .isEqualTo("product:list:c0:b0:sPRICE_ASC:p1:sz10"),
            () -> assertThat(productListCacheRepository.generateKey(5L, null, ProductSortType.LIKES_DESC, 0, 20))
                .isEqualTo("product:list:c5:b0:sLIKES_DESC:p0:sz20")
        );
    }

    private Page<ProductInfo> createProductInfoPage() {
        BrandInfo brandInfo = new BrandInfo(1L, "Apple", "애플", "https://example.com/apple.png", 100L);

        List<ProductInfo> products = List.of(
            new ProductInfo(1L, "아이폰 15", "PROD-001", 1500000L, 1400000L,
                ProductStatus.SALE, 100000L, DiscountType.PRICE, brandInfo, 10L),
            new ProductInfo(2L, "맥북 프로", "PROD-002", 3000000L, 2700000L,
                ProductStatus.SALE, 300000L, DiscountType.PRICE, brandInfo, 20L)
        );

        return new PageImpl<>(products, PageRequest.of(0, 20), 2);
    }
}
