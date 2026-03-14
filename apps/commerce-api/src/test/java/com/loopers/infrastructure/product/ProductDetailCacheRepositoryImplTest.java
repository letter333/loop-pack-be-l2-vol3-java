package com.loopers.infrastructure.product;

import com.loopers.application.brand.BrandInfo;
import com.loopers.application.product.ProductDetailCacheRepository;
import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductImageInfo;
import com.loopers.application.product.ProductOptionInfo;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.ImageType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("ProductDetailCacheRepositoryImpl 통합 테스트")
class ProductDetailCacheRepositoryImplTest {

    @Autowired
    private ProductDetailCacheRepository productDetailCacheRepository;

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
        Optional<ProductDetailInfo> result = productDetailCacheRepository.get(999L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("캐시에 저장 후 정상적으로 조회한다")
    void returnsProductDetailInfo_afterPut() {
        // Arrange
        ProductDetailInfo info = createProductDetailInfo();

        // Act
        productDetailCacheRepository.put(1L, info);
        Optional<ProductDetailInfo> result = productDetailCacheRepository.get(1L);

        // Assert
        assertThat(result).isPresent();
        ProductDetailInfo cached = result.get();
        assertAll(
            () -> assertThat(cached.id()).isEqualTo(1L),
            () -> assertThat(cached.name()).isEqualTo("아이폰 15"),
            () -> assertThat(cached.productCode()).isEqualTo("PROD-001"),
            () -> assertThat(cached.basePrice()).isEqualTo(1500000L),
            () -> assertThat(cached.discountedPrice()).isEqualTo(1400000L),
            () -> assertThat(cached.status()).isEqualTo(ProductStatus.SALE),
            () -> assertThat(cached.discount()).isEqualTo(100000L),
            () -> assertThat(cached.discountType()).isEqualTo(DiscountType.PRICE),
            () -> assertThat(cached.brand()).isNotNull(),
            () -> assertThat(cached.brand().name()).isEqualTo("Apple"),
            () -> assertThat(cached.likeCount()).isEqualTo(10L),
            () -> assertThat(cached.options()).hasSize(1),
            () -> assertThat(cached.options().get(0).optionValue()).isEqualTo("256GB"),
            () -> assertThat(cached.images()).hasSize(1),
            () -> assertThat(cached.images().get(0).type()).isEqualTo(ImageType.MAIN)
        );
    }

    @Test
    @DisplayName("캐시 삭제 후 Optional.empty()를 반환한다")
    void returnsEmpty_afterEvict() {
        // Arrange
        ProductDetailInfo info = createProductDetailInfo();
        productDetailCacheRepository.put(1L, info);

        // Act
        productDetailCacheRepository.evict(1L);
        Optional<ProductDetailInfo> result = productDetailCacheRepository.get(1L);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("캐시 저장 시 TTL이 10분으로 설정된다")
    void setsTtlTo10Minutes() {
        // Arrange
        ProductDetailInfo info = createProductDetailInfo();

        // Act
        productDetailCacheRepository.put(1L, info);

        // Assert
        Long ttl = redisTemplate.getExpire("product:detail:1", TimeUnit.SECONDS);
        assertThat(ttl).isNotNull();
        assertThat(ttl).isBetween(590L, 600L);
    }

    private ProductDetailInfo createProductDetailInfo() {
        BrandInfo brandInfo = new BrandInfo(1L, "Apple", "애플", "https://example.com/apple.png", 100L);
        List<ProductOptionInfo> options = List.of(
            new ProductOptionInfo(1L, "256GB", "256GB", 0L, 100)
        );
        List<ProductImageInfo> images = List.of(
            new ProductImageInfo(1L, ImageType.MAIN, "https://example.com/main.jpg", "메인 이미지")
        );
        return new ProductDetailInfo(
            1L, "아이폰 15", "PROD-001", 1500000L, 1400000L,
            ProductStatus.SALE, 100000L, DiscountType.PRICE,
            brandInfo, 10L, options, images
        );
    }
}
