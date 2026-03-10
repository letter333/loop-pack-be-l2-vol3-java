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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("ProductService 통합 테스트")
class ProductServiceTest {

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

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        @DisplayName("존재하는 상품을 조회하면 Product를 반환한다")
        void returnsProduct_whenProductExists() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.getProduct(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getName()).isEqualTo("아이폰 15")
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.getProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getActiveProduct")
    class GetActiveProduct {

        @Test
        @DisplayName("활성 상품을 조회하면 Product를 반환한다")
        void returnsProduct_whenProductIsActive() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.getActiveProduct(saved.getId());

            // Assert
            assertThat(result.getName()).isEqualTo("아이폰 15");
        }

        @Test
        @DisplayName("삭제된 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductIsDeleted() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productService.deleteProduct(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> productService.getActiveProduct(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.getActiveProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        @DisplayName("상품을 정상적으로 생성한다")
        void createsProduct() {
            // Act
            Product result = productService.createProduct("아이폰 15", 1L, 1L, 1500000L);

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("아이폰 15"),
                () -> assertThat(result.getBrandId()).isEqualTo(1L),
                () -> assertThat(result.getCategoryId()).isEqualTo(1L),
                () -> assertThat(result.getBasePrice()).isEqualTo(1500000L),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.SALE),
                () -> assertThat(result.getProductCode()).matches("\\d{8}-\\d{5}")
            );
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        @DisplayName("상품을 정상적으로 수정한다")
        void updatesProduct() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.updateProduct(
                saved.getId(), "아이폰 15 Pro", 2L, 1800000L,
                100000L, DiscountType.PRICE, ProductStatus.STOP
            );

            // Assert
            assertAll(
                () -> assertThat(result.getName()).isEqualTo("아이폰 15 Pro"),
                () -> assertThat(result.getCategoryId()).isEqualTo(2L),
                () -> assertThat(result.getBasePrice()).isEqualTo(1800000L),
                () -> assertThat(result.getDiscount()).isEqualTo(100000L),
                () -> assertThat(result.getDiscountType()).isEqualTo(DiscountType.PRICE),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.STOP)
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.updateProduct(
                999L, "아이폰 15 Pro", 2L, 1800000L,
                null, null, ProductStatus.SALE
            ))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        @DisplayName("상품을 삭제하면 Soft Delete 된다")
        void deletesProduct() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            productService.deleteProduct(saved.getId());

            // Assert
            Product deleted = productService.getProduct(saved.getId());
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getProducts")
    class GetProducts {

        @Test
        @DisplayName("전체 상품 목록을 페이지로 조회한다")
        void returnsPagedProducts_whenNoFilter() {
            // Arrange
            productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            productRepository.save(new Product("맥북 프로", 1L, 2L, 3000000L));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, null, ProductSortType.LATEST, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent()).hasSize(3),
                () -> assertThat(result.getTotalElements()).isEqualTo(3),
                () -> assertThat(result.getTotalPages()).isEqualTo(1)
            );
        }

        @Test
        @DisplayName("카테고리 ID로 필터링하여 상품 목록을 조회한다")
        void returnsFilteredProducts_whenCategoryIdProvided() {
            // Arrange
            productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            productRepository.save(new Product("맥북 프로", 1L, 2L, 3000000L));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(1L, null, null, ProductSortType.LATEST, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent()).hasSize(2),
                () -> assertThat(result.getContent()).allMatch(p -> p.getCategoryId().equals(1L))
            );
        }

        @Test
        @DisplayName("키워드로 검색하여 상품 목록을 조회한다")
        void returnsFilteredProducts_whenKeywordProvided() {
            // Arrange
            productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            productRepository.save(new Product("아이폰 15 Pro", 1L, 1L, 1800000L));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, "아이폰", ProductSortType.LATEST, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent()).hasSize(2),
                () -> assertThat(result.getContent()).allMatch(p -> p.getName().contains("아이폰"))
            );
        }

        @Test
        @DisplayName("최신순으로 정렬하여 조회한다")
        void returnsProducts_sortedByLatest() {
            // Arrange
            Product product1 = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            Product product2 = productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            Product product3 = productRepository.save(new Product("맥북 프로", 1L, 2L, 3000000L));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, null, ProductSortType.LATEST, pageable);

            // Assert
            assertThat(result.getContent().get(0).getId()).isEqualTo(product3.getId());
        }

        @Test
        @DisplayName("가격 낮은순으로 정렬하여 조회한다")
        void returnsProducts_sortedByPriceAsc() {
            // Arrange
            productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            productRepository.save(new Product("맥북 프로", 1L, 2L, 3000000L));
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, null, ProductSortType.PRICE_ASC, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent().get(0).getBasePrice()).isEqualTo(1400000L),
                () -> assertThat(result.getContent().get(1).getBasePrice()).isEqualTo(1500000L),
                () -> assertThat(result.getContent().get(2).getBasePrice()).isEqualTo(3000000L)
            );
        }

        @Test
        @DisplayName("페이징이 정상적으로 동작한다")
        void returnsPagedProducts_withPagination() {
            // Arrange
            for (int i = 0; i < 25; i++) {
                productRepository.save(new Product("상품" + i, 1L, 1L, 1000000L + i));
            }
            Pageable firstPage = PageRequest.of(0, 10);
            Pageable secondPage = PageRequest.of(1, 10);
            Pageable thirdPage = PageRequest.of(2, 10);

            // Act
            Page<Product> firstResult = productService.getProducts(null, null, null, ProductSortType.LATEST, firstPage);
            Page<Product> secondResult = productService.getProducts(null, null, null, ProductSortType.LATEST, secondPage);
            Page<Product> thirdResult = productService.getProducts(null, null, null, ProductSortType.LATEST, thirdPage);

            // Assert
            assertAll(
                () -> assertThat(firstResult.getContent()).hasSize(10),
                () -> assertThat(secondResult.getContent()).hasSize(10),
                () -> assertThat(thirdResult.getContent()).hasSize(5),
                () -> assertThat(firstResult.getTotalElements()).isEqualTo(25),
                () -> assertThat(firstResult.getTotalPages()).isEqualTo(3)
            );
        }

        @Test
        @DisplayName("삭제된 상품은 조회되지 않는다")
        void excludesDeletedProducts() {
            // Arrange
            Product activeProduct = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            Product toDelete = productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            productService.deleteProduct(toDelete.getId());
            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, null, ProductSortType.LATEST, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent()).hasSize(1),
                () -> assertThat(result.getContent().get(0).getId()).isEqualTo(activeProduct.getId())
            );
        }

        @Test
        @DisplayName("좋아요 많은순으로 정렬하여 조회한다")
        void returnsProducts_sortedByLikesDesc() {
            // Arrange
            Product product1 = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            Product product2 = productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            Product product3 = productRepository.save(new Product("맥북 프로", 1L, 2L, 3000000L));

            // 좋아요 수 설정: product2(5) > product1(3) > product3(1)
            for (int i = 0; i < 3; i++) productService.increaseLikeCount(product1.getId());
            for (int i = 0; i < 5; i++) productService.increaseLikeCount(product2.getId());
            productService.increaseLikeCount(product3.getId());

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, null, ProductSortType.LIKES_DESC, pageable);

            // Assert
            assertAll(
                () -> assertThat(result.getContent().get(0).getId()).isEqualTo(product2.getId()),
                () -> assertThat(result.getContent().get(0).getLikeCount()).isEqualTo(5L),
                () -> assertThat(result.getContent().get(1).getId()).isEqualTo(product1.getId()),
                () -> assertThat(result.getContent().get(1).getLikeCount()).isEqualTo(3L),
                () -> assertThat(result.getContent().get(2).getId()).isEqualTo(product3.getId()),
                () -> assertThat(result.getContent().get(2).getLikeCount()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("좋아요 수가 같으면 최신순으로 정렬한다")
        void returnsProducts_sortedByCreatedAtDesc_whenLikeCountIsSame() {
            // Arrange
            Product product1 = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            Product product2 = productRepository.save(new Product("갤럭시 S24", 2L, 1L, 1400000L));
            Product product3 = productRepository.save(new Product("맥북 프로", 1L, 2L, 3000000L));

            // 모든 상품 좋아요 수 동일하게 설정
            productService.increaseLikeCount(product1.getId());
            productService.increaseLikeCount(product2.getId());
            productService.increaseLikeCount(product3.getId());

            Pageable pageable = PageRequest.of(0, 10);

            // Act
            Page<Product> result = productService.getProducts(null, null, null, ProductSortType.LIKES_DESC, pageable);

            // Assert - 좋아요 수 동일하면 최신순 (product3 > product2 > product1)
            assertAll(
                () -> assertThat(result.getContent().get(0).getId()).isEqualTo(product3.getId()),
                () -> assertThat(result.getContent().get(1).getId()).isEqualTo(product2.getId()),
                () -> assertThat(result.getContent().get(2).getId()).isEqualTo(product1.getId())
            );
        }
    }

    @Nested
    @DisplayName("validateProduct")
    class ValidateProduct {

        @Test
        @DisplayName("SALE 상태 상품은 검증을 통과한다")
        void passesValidation_whenProductIsSale() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Product result = productService.validateProduct(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.SALE)
            );
        }

        @Test
        @DisplayName("STOP 상태 상품은 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenProductIsStop() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productService.updateProduct(
                saved.getId(), "아이폰 15", 1L, 1500000L,
                null, null, ProductStatus.STOP
            );

            // Act & Assert
            assertThatThrownBy(() -> productService.validateProduct(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("SOLDOUT 상태 상품은 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenProductIsSoldout() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productService.updateProduct(
                saved.getId(), "아이폰 15", 1L, 1500000L,
                null, null, ProductStatus.SOLDOUT
            );

            // Act & Assert
            assertThatThrownBy(() -> productService.validateProduct(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("삭제된 상품은 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductIsDeleted() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productService.deleteProduct(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> productService.validateProduct(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("존재하지 않는 상품은 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> productService.validateProduct(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("increaseStock")
    class IncreaseStock {

        @Test
        @DisplayName("재고를 정상적으로 증가시킨다")
        void increasesStock() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 100)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
            Long optionId = saved.getOptions().get(0).getId();

            // Act
            productService.increaseStock(saved.getId(), optionId, 50);

            // Assert
            Product result = productService.getProduct(saved.getId());
            assertThat(result.getOption(optionId).getStockQuantity()).isEqualTo(150);
        }

        @Test
        @DisplayName("재고 증가 시 상한선을 초과하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStockExceedsMaxValue() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, Integer.MAX_VALUE - 10)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
            Long optionId = saved.getOptions().get(0).getId();

            // Act & Assert
            assertThatThrownBy(() -> productService.increaseStock(saved.getId(), optionId, 20))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("존재하지 않는 옵션의 재고를 증가시키면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenOptionNotExists() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act & Assert
            assertThatThrownBy(() -> productService.increaseStock(saved.getId(), 999L, 10))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("SOLDOUT 상태에서 재고가 증가하면 SALE로 복구된다")
        void changesStatusToSale_whenStockRestored() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 0)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
            Long optionId = saved.getOptions().get(0).getId();
            // 상품을 SOLDOUT 상태로 변경
            productService.updateProduct(saved.getId(), "아이폰 15", 1L, 1500000L, null, null, ProductStatus.SOLDOUT);

            // Act
            productService.increaseStock(saved.getId(), optionId, 10);

            // Assert
            Product result = productService.getProduct(saved.getId());
            assertAll(
                () -> assertThat(result.getOption(optionId).getStockQuantity()).isEqualTo(10),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.SALE)
            );
        }

        @Test
        @DisplayName("STOP 상태에서 재고가 증가해도 STOP 상태를 유지한다")
        void maintainsStopStatus_whenStockRestored() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 0)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
            Long optionId = saved.getOptions().get(0).getId();
            // 상품을 STOP 상태로 변경
            productService.updateProduct(saved.getId(), "아이폰 15", 1L, 1500000L, null, null, ProductStatus.STOP);

            // Act
            productService.increaseStock(saved.getId(), optionId, 10);

            // Assert
            Product result = productService.getProduct(saved.getId());
            assertAll(
                () -> assertThat(result.getOption(optionId).getStockQuantity()).isEqualTo(10),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.STOP)
            );
        }
    }

    @Nested
    @DisplayName("decreaseStock")
    class DecreaseStock {

        @Test
        @DisplayName("재고를 정상적으로 감소시킨다")
        void decreasesStock() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 100)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
            Long optionId = saved.getOptions().get(0).getId();

            // Act
            productService.decreaseStock(saved.getId(), optionId, 30);

            // Assert
            Product result = productService.getProduct(saved.getId());
            assertThat(result.getOption(optionId).getStockQuantity()).isEqualTo(70);
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        void throwsBadRequest_whenStockIsInsufficient() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 10)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
            Long optionId = saved.getOptions().get(0).getId();

            // Act & Assert
            assertThatThrownBy(() -> productService.decreaseStock(saved.getId(), optionId, 20))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
        }

        @Test
        @DisplayName("존재하지 않는 옵션의 재고를 감소시키면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenOptionNotExists() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act & Assert
            assertThatThrownBy(() -> productService.decreaseStock(saved.getId(), 999L, 10))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("모든 옵션의 재고가 0이 되면 SOLDOUT으로 상태가 변경된다")
        void changesStatusToSoldout_whenAllStockDepleted() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 10),
                new ProductOption(null, "WHITE_M", "화이트 / M", 5000L, 5)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));

            // 다시 조회하여 저장된 옵션 ID 확인 (optionValue로 식별)
            Product loaded = productService.getProduct(saved.getId());
            ProductOption blackOption = loaded.getOptions().stream()
                .filter(o -> o.getOptionValue().equals("BLACK_M"))
                .findFirst().orElseThrow();
            ProductOption whiteOption = loaded.getOptions().stream()
                .filter(o -> o.getOptionValue().equals("WHITE_M"))
                .findFirst().orElseThrow();

            // Act - 모든 재고 소진
            productService.decreaseStock(saved.getId(), blackOption.getId(), 10);
            productService.decreaseStock(saved.getId(), whiteOption.getId(), 5);

            // Assert
            Product result = productService.getProduct(saved.getId());
            assertAll(
                () -> assertThat(result.getOption(blackOption.getId()).getStockQuantity()).isEqualTo(0),
                () -> assertThat(result.getOption(whiteOption.getId()).getStockQuantity()).isEqualTo(0),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.SOLDOUT)
            );
        }

        @Test
        @DisplayName("일부 옵션의 재고만 0이면 SALE 상태를 유지한다")
        void maintainsSaleStatus_whenSomeOptionsHaveStock() {
            // Arrange
            List<ProductOption> options = List.of(
                new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, 10),
                new ProductOption(null, "WHITE_M", "화이트 / M", 5000L, 5)
            );
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));

            // 다시 조회하여 저장된 옵션 ID 확인 (optionValue로 식별)
            Product loaded = productService.getProduct(saved.getId());
            ProductOption blackOption = loaded.getOptions().stream()
                .filter(o -> o.getOptionValue().equals("BLACK_M"))
                .findFirst().orElseThrow();

            // Act - 첫 번째 옵션만 재고 소진
            productService.decreaseStock(saved.getId(), blackOption.getId(), 10);

            // Assert
            Product result = productService.getProduct(saved.getId());
            assertAll(
                () -> assertThat(result.getOption(blackOption.getId()).getStockQuantity()).isEqualTo(0),
                () -> assertThat(result.getStatus()).isEqualTo(ProductStatus.SALE)
            );
        }
    }

    @Nested
    @DisplayName("increaseLikeCount")
    class IncreaseLikeCount {

        @Test
        @DisplayName("좋아요 수를 정상적으로 증가시킨다")
        void increasesLikeCount() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Long result = productService.increaseLikeCount(saved.getId());

            // Assert
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("좋아요 수를 여러 번 증가시키면 누적된다")
        void accumulates_whenIncreasedMultipleTimes() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            productService.increaseLikeCount(saved.getId());
            productService.increaseLikeCount(saved.getId());
            Long result = productService.increaseLikeCount(saved.getId());

            // Assert
            assertThat(result).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("decreaseLikeCount")
    class DecreaseLikeCount {

        @Test
        @DisplayName("좋아요 수를 정상적으로 감소시킨다")
        void decreasesLikeCount() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));
            productService.increaseLikeCount(saved.getId());
            productService.increaseLikeCount(saved.getId());

            // Act
            Long result = productService.decreaseLikeCount(saved.getId());

            // Assert
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("좋아요 수가 0이면 0 미만으로 감소하지 않는다")
        void doesNotGoBelowZero() {
            // Arrange
            Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L));

            // Act
            Long result = productService.decreaseLikeCount(saved.getId());

            // Assert
            assertThat(result).isEqualTo(0L);
        }
    }
}