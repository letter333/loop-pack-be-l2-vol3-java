package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("ProductFacade 통합 테스트")
class ProductFacadeTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Brand savedBrand;

    @BeforeEach
    void setUp() {
        savedBrand = brandRepository.save(new Brand("Apple", "애플", "https://example.com/apple.png"));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("상품 정보를 브랜드 정보와 함께 조회한다")
        void returnsProductInfoWithBrand() {
            // Arrange
            Product product = productRepository.save(
                new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L)
            );

            // Act
            ProductInfo result = productFacade.getProduct(product.getId());

            // Assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(product.getId()),
                () -> assertThat(result.name()).isEqualTo("아이폰 15"),
                () -> assertThat(result.brand()).isNotNull(),
                () -> assertThat(result.brand().name()).isEqualTo("Apple"),
                () -> assertThat(result.likeCount()).isEqualTo(0L)
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productFacade.getProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("페이지로 상품 목록을 조회하고 브랜드 정보를 포함한다")
        void returnsPagedProductsWithBrandInfo() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            productRepository.save(new Product("아이폰 14", savedBrand.getId(), 1L, 1200000L));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<ProductInfo> result = productFacade.getProducts(null, null, ProductSortType.LATEST, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent()).hasSize(2),
                () -> assertThat(result.getTotalElements()).isEqualTo(2),
                () -> assertThat(result.getContent()).allMatch(info -> info.brand() != null),
                () -> assertThat(result.getContent()).allMatch(info -> info.brand().name().equals("Apple"))
            );
        }

        @Test
        @DisplayName("페이징 정보가 정상적으로 반환된다")
        void returnsCorrectPagingInfo() {
            // Arrange
            for (int i = 0; i < 25; i++) {
                productRepository.save(new Product("상품" + i, savedBrand.getId(), 1L, 1000000L + i));
            }
            Pageable pageable = PageRequest.of(0, 20);

            // Act
            Page<ProductInfo> result = productFacade.getProducts(null, null, ProductSortType.LATEST, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent()).hasSize(20),
                () -> assertThat(result.getTotalElements()).isEqualTo(25),
                () -> assertThat(result.getTotalPages()).isEqualTo(2),
                () -> assertThat(result.isFirst()).isTrue(),
                () -> assertThat(result.hasNext()).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("createProduct (Admin)")
    class CreateProduct {

        @Test
        @DisplayName("관리자가 상품을 정상적으로 생성한다")
        void createsProduct() {
            // Arrange
            ProductCommand.Create command = new ProductCommand.Create(
                "아이폰 15", savedBrand.getId(), 1L, 1500000L
            );

            // Act
            ProductDetailInfo result = productFacade.createProduct("loopers.admin", command);

            // Assert
            assertAll(
                () -> assertThat(result.id()).isNotNull(),
                () -> assertThat(result.name()).isEqualTo("아이폰 15"),
                () -> assertThat(result.brandId()).isEqualTo(savedBrand.getId()),
                () -> assertThat(result.status()).isEqualTo(ProductStatus.SALE)
            );
        }

        @Test
        @DisplayName("관리자가 아니면 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNotAdmin() {
            // Arrange
            ProductCommand.Create command = new ProductCommand.Create(
                "아이폰 15", savedBrand.getId(), 1L, 1500000L
            );

            // Act & Assert
            assertThatThrownBy(() -> productFacade.createProduct("invalid.ldap", command))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("deleteProduct (Admin)")
    class DeleteProduct {

        @Test
        @DisplayName("관리자가 상품을 삭제한다")
        void deletesProduct() {
            // Arrange
            Product product = productRepository.save(
                new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L)
            );

            // Act
            productFacade.deleteProduct("loopers.admin", product.getId());

            // Assert
            assertThatThrownBy(() -> productFacade.getProduct(product.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }
}