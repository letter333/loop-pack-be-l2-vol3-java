package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("ProductImageService 통합 테스트")
class ProductImageServiceTest {

    @Autowired
    private ProductImageService productImageService;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getProductImage")
    class GetProductImage {

        @Test
        @DisplayName("존재하는 이미지를 조회하면 ProductImage를 반환한다")
        void returnsProductImage_whenImageExists() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            ProductImage saved = productImageRepository.save(
                new ProductImage(product.getId(), ImageType.MAIN, "https://example.com/image.jpg", "메인 이미지")
            );

            // Act
            ProductImage result = productImageService.getProductImage(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getUrl()).isEqualTo("https://example.com/image.jpg")
            );
        }

        @Test
        @DisplayName("존재하지 않는 이미지를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenImageNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productImageService.getProductImage(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getProductImages")
    class GetProductImages {

        @Test
        @DisplayName("상품의 이미지 목록을 조회한다")
        void returnsProductImages_whenProductHasImages() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productImageRepository.save(new ProductImage(product.getId(), ImageType.MAIN, "https://example.com/main.jpg", "메인"));
            productImageRepository.save(new ProductImage(product.getId(), ImageType.SUB, "https://example.com/sub.jpg", "서브"));

            // Act
            List<ProductImage> result = productImageService.getProductImages(product.getId());

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("이미지가 없는 상품은 빈 목록을 반환한다")
        void returnsEmptyList_whenProductHasNoImages() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            List<ProductImage> result = productImageService.getProductImages(product.getId());

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createProductImage")
    class CreateProductImage {

        @Test
        @DisplayName("이미지를 정상적으로 생성한다")
        void createsProductImage() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            ProductImage result = productImageService.createProductImage(
                product.getId(), ImageType.MAIN, "https://example.com/image.jpg", "메인 이미지"
            );

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getProductId()).isEqualTo(product.getId()),
                () -> assertThat(result.getType()).isEqualTo(ImageType.MAIN),
                () -> assertThat(result.getUrl()).isEqualTo("https://example.com/image.jpg"),
                () -> assertThat(result.getAltText()).isEqualTo("메인 이미지")
            );
        }
    }

    @Nested
    @DisplayName("deleteProductImage")
    class DeleteProductImage {

        @Test
        @DisplayName("이미지를 정상적으로 삭제한다")
        void deletesProductImage() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            ProductImage saved = productImageRepository.save(
                new ProductImage(product.getId(), ImageType.MAIN, "https://example.com/image.jpg", "메인 이미지")
            );

            // Act
            productImageService.deleteProductImage(saved.getId());

            // Assert
            assertThatThrownBy(() -> productImageService.getProductImage(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }
}