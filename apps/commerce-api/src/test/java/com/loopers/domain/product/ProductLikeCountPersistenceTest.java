package com.loopers.domain.product;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("상품 좋아요 수 원자적 UPDATE DB 반영 검증 테스트")
class ProductLikeCountPersistenceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("좋아요 증가 후 상품 조회 시 like_count가 1로 반영된다")
    void increaseLikeCount_thenGetProduct_returnsUpdatedCount() {
        // Arrange
        Product saved = productRepository.save(new Product("테스트 상품", 1L, 1L, 10000L, null, null));
        Long productId = saved.getId();

        // Act
        productService.increaseLikeCount(productId);

        // Assert
        Product result = productService.getProduct(productId);
        assertThat(result.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("좋아요 감소 후 상품 조회 시 like_count가 정확히 반영된다")
    void decreaseLikeCount_thenGetProduct_returnsUpdatedCount() {
        // Arrange
        Product saved = productRepository.save(new Product("테스트 상품", 1L, 1L, 10000L, null, null));
        Long productId = saved.getId();
        for (int i = 0; i < 3; i++) {
            productService.increaseLikeCount(productId);
        }

        // Act
        productService.decreaseLikeCount(productId);

        // Assert
        Product result = productService.getProduct(productId);
        assertThat(result.getLikeCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("like_count가 0일 때 감소해도 음수로 내려가지 않는다")
    void decreaseLikeCount_whenZero_remainsZero() {
        // Arrange
        Product saved = productRepository.save(new Product("테스트 상품", 1L, 1L, 10000L, null, null));
        Long productId = saved.getId();

        // Act
        productService.decreaseLikeCount(productId);

        // Assert
        Product result = productService.getProduct(productId);
        assertThat(result.getLikeCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("여러 번 증가 후 정확한 누적값이 반영된다")
    void increaseLikeCount_multipleTimes_returnsAccumulatedCount() {
        // Arrange
        Product saved = productRepository.save(new Product("테스트 상품", 1L, 1L, 10000L, null, null));
        Long productId = saved.getId();

        // Act
        for (int i = 0; i < 5; i++) {
            productService.increaseLikeCount(productId);
        }

        // Assert
        Product result = productService.getProduct(productId);
        assertThat(result.getLikeCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("증가 후 감소 사이클에서 정확한 값이 유지된다")
    void increaseAndDecreaseCycle_maintainsCorrectCount() {
        // Arrange
        Product saved = productRepository.save(new Product("테스트 상품", 1L, 1L, 10000L, null, null));
        Long productId = saved.getId();

        // Act
        for (int i = 0; i < 3; i++) {
            productService.increaseLikeCount(productId);
        }
        for (int i = 0; i < 2; i++) {
            productService.decreaseLikeCount(productId);
        }

        // Assert
        Product result = productService.getProduct(productId);
        assertThat(result.getLikeCount()).isEqualTo(1L);
    }
}
