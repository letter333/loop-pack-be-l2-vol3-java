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
@DisplayName("ProductOptionService 통합 테스트")
class ProductOptionServiceTest {

    @Autowired
    private ProductOptionService productOptionService;

    @Autowired
    private ProductOptionRepository productOptionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getProductOption")
    class GetProductOption {

        @Test
        @DisplayName("존재하는 옵션을 조회하면 ProductOption을 반환한다")
        void returnsProductOption_whenOptionExists() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            ProductOption saved = productOptionRepository.save(
                new ProductOption(product.getId(), "BLACK_M", "블랙 / M", 5000L, 100)
            );

            // Act
            ProductOption result = productOptionService.getProductOption(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getOptionValue()).isEqualTo("BLACK_M")
            );
        }

        @Test
        @DisplayName("존재하지 않는 옵션을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenOptionNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productOptionService.getProductOption(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getProductOptions")
    class GetProductOptions {

        @Test
        @DisplayName("상품의 옵션 목록을 조회한다")
        void returnsProductOptions_whenProductHasOptions() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productOptionRepository.save(new ProductOption(product.getId(), "BLACK_M", "블랙 / M", 5000L, 100));
            productOptionRepository.save(new ProductOption(product.getId(), "WHITE_L", "화이트 / L", 3000L, 50));

            // Act
            List<ProductOption> result = productOptionService.getProductOptions(product.getId());

            // Assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("옵션이 없는 상품은 빈 목록을 반환한다")
        void returnsEmptyList_whenProductHasNoOptions() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            List<ProductOption> result = productOptionService.getProductOptions(product.getId());

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createProductOption")
    class CreateProductOption {

        @Test
        @DisplayName("옵션을 정상적으로 생성한다")
        void createsProductOption() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            ProductOption result = productOptionService.createProductOption(
                product.getId(), "BLACK_M", "블랙 / M", 5000L, 100
            );

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getProductId()).isEqualTo(product.getId()),
                () -> assertThat(result.getOptionValue()).isEqualTo("BLACK_M"),
                () -> assertThat(result.getDisplayName()).isEqualTo("블랙 / M"),
                () -> assertThat(result.getExtraPrice()).isEqualTo(5000L),
                () -> assertThat(result.getStockQuantity()).isEqualTo(100)
            );
        }
    }

    @Nested
    @DisplayName("decreaseStock")
    class DecreaseStock {

        @Test
        @DisplayName("재고를 정상적으로 차감한다")
        void decreasesStock() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            ProductOption saved = productOptionRepository.save(
                new ProductOption(product.getId(), "BLACK_M", "블랙 / M", 5000L, 100)
            );

            // Act
            productOptionService.decreaseStock(saved.getId(), 30);

            // Assert
            ProductOption result = productOptionService.getProductOption(saved.getId());
            assertThat(result.getStockQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStockIsInsufficient() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            ProductOption saved = productOptionRepository.save(
                new ProductOption(product.getId(), "BLACK_M", "블랙 / M", 5000L, 10)
            );

            // Act & Assert
            assertThatThrownBy(() -> productOptionService.decreaseStock(saved.getId(), 20))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("존재하지 않는 옵션이면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenOptionNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productOptionService.decreaseStock(999L, 10))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("increaseStock")
    class IncreaseStock {

        @Test
        @DisplayName("재고를 정상적으로 증가한다")
        void increasesStock() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            ProductOption saved = productOptionRepository.save(
                new ProductOption(product.getId(), "BLACK_M", "블랙 / M", 5000L, 100)
            );

            // Act
            productOptionService.increaseStock(saved.getId(), 50);

            // Assert
            ProductOption result = productOptionService.getProductOption(saved.getId());
            assertThat(result.getStockQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("존재하지 않는 옵션이면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenOptionNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productOptionService.increaseStock(999L, 10))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }
}