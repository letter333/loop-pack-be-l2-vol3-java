package com.loopers.domain.brand;

import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("BrandService 통합 테스트")
class BrandServiceTest {

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getBrand")
    class GetBrand {

        @Test
        @DisplayName("존재하는 브랜드를 조회하면 Brand를 반환한다")
        void returnsBrand_whenBrandExists() {
            // Arrange
            Brand saved = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));

            // Act
            Brand result = brandService.getBrand(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getName()).isEqualTo("Nike")
            );
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandNotExists() {
            // Arrange
            Long nonExistentId = 999L;

            // Act & Assert
            assertThatThrownBy(() -> brandService.getBrand(nonExistentId))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getActiveBrand")
    class GetActiveBrand {

        @Test
        @DisplayName("활성 브랜드를 조회하면 Brand를 반환한다")
        void returnsBrand_whenBrandIsActive() {
            // Arrange
            Brand saved = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));

            // Act
            Brand result = brandService.getActiveBrand(saved.getId());

            // Assert
            assertThat(result.getName()).isEqualTo("Nike");
        }

        @Test
        @DisplayName("삭제된 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandIsDeleted() {
            // Arrange
            Brand brand = new Brand("Nike", "스포츠 브랜드", "https://logo.png");
            Brand saved = brandRepository.save(brand);
            brandService.deleteBrand(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> brandService.getActiveBrand(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getBrands")
    class GetBrands {

        @Test
        @DisplayName("삭제되지 않은 브랜드만 조회한다")
        void returnsOnlyActiveBrands() {
            // Arrange
            brandRepository.save(new Brand("Nike", "설명1", "https://logo1.png"));
            brandRepository.save(new Brand("Adidas", "설명2", "https://logo2.png"));
            Brand toDelete = brandRepository.save(new Brand("Puma", "설명3", "https://logo3.png"));
            brandService.deleteBrand(toDelete.getId());

            // Act
            Page<Brand> result = brandService.getBrands(PageRequest.of(0, 10));

            // Assert
            assertAll(
                () -> assertThat(result.getTotalElements()).isEqualTo(2),
                () -> assertThat(result.getContent()).extracting(Brand::getName)
                    .containsExactlyInAnyOrder("Nike", "Adidas")
            );
        }

        @Test
        @DisplayName("페이징이 정상 동작한다")
        void returnsPaginatedResults() {
            // Arrange
            for (int i = 1; i <= 25; i++) {
                brandRepository.save(new Brand("Brand" + i, "설명" + i, "https://logo" + i + ".png"));
            }

            // Act
            Page<Brand> page1 = brandService.getBrands(PageRequest.of(0, 10));
            Page<Brand> page2 = brandService.getBrands(PageRequest.of(1, 10));
            Page<Brand> page3 = brandService.getBrands(PageRequest.of(2, 10));

            // Assert
            assertAll(
                () -> assertThat(page1.getContent()).hasSize(10),
                () -> assertThat(page2.getContent()).hasSize(10),
                () -> assertThat(page3.getContent()).hasSize(5),
                () -> assertThat(page1.getTotalElements()).isEqualTo(25),
                () -> assertThat(page1.getTotalPages()).isEqualTo(3)
            );
        }
    }

    @Nested
    @DisplayName("createBrand")
    class CreateBrand {

        @Test
        @DisplayName("브랜드를 정상적으로 생성한다")
        void createsBrand() {
            // Arrange & Act
            Brand result = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("Nike"),
                () -> assertThat(result.getDescription()).isEqualTo("스포츠 브랜드"),
                () -> assertThat(result.getLogoImageUrl()).isEqualTo("https://logo.png")
            );
        }
    }

    @Nested
    @DisplayName("updateBrand")
    class UpdateBrand {

        @Test
        @DisplayName("브랜드 정보를 정상적으로 수정한다")
        void updatesBrand() {
            // Arrange
            Brand saved = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));

            // Act
            Brand result = brandService.updateBrand(saved.getId(), "Adidas", "독일 브랜드", "https://adidas.png");

            // Assert
            assertAll(
                () -> assertThat(result.getName()).isEqualTo("Adidas"),
                () -> assertThat(result.getDescription()).isEqualTo("독일 브랜드"),
                () -> assertThat(result.getLogoImageUrl()).isEqualTo("https://adidas.png")
            );
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 수정하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandNotExists() {
            // Arrange
            Long nonExistentId = 999L;

            // Act & Assert
            assertThatThrownBy(() -> brandService.updateBrand(nonExistentId, "Adidas", "설명", "https://logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteBrand")
    class DeleteBrand {

        @Test
        @DisplayName("브랜드를 삭제하면 Soft Delete 된다")
        void deletesBrand() {
            // Arrange
            Brand saved = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));

            // Act
            brandService.deleteBrand(saved.getId());

            // Assert
            Brand deleted = brandService.getBrand(saved.getId());
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 삭제하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandNotExists() {
            // Arrange
            Long nonExistentId = 999L;

            // Act & Assert
            assertThatThrownBy(() -> brandService.deleteBrand(nonExistentId))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("브랜드 삭제 시 연관된 상품도 함께 Soft Delete 된다")
        void deletesRelatedProducts_whenBrandDeleted() {
            // Arrange
            Brand brand = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));
            Category category = categoryRepository.save(new Category("스포츠"));
            Product product1 = productService.createProduct("나이키 신발", brand.getId(), category.getId(), 100000L);
            Product product2 = productService.createProduct("나이키 가방", brand.getId(), category.getId(), 50000L);

            // Act
            brandService.deleteBrand(brand.getId());

            // Assert
            Product deletedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product deletedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            assertAll(
                () -> assertThat(deletedProduct1.isDeleted()).isTrue(),
                () -> assertThat(deletedProduct2.isDeleted()).isTrue()
            );
        }

        @Test
        @DisplayName("연관 상품이 없는 브랜드 삭제 시 정상 동작한다")
        void deletesSuccessfully_whenNoRelatedProducts() {
            // Arrange
            Brand brand = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));

            // Act
            brandService.deleteBrand(brand.getId());

            // Assert
            Brand deleted = brandService.getBrand(brand.getId());
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("다른 브랜드의 상품은 영향받지 않는다")
        void doesNotAffectOtherBrandProducts_whenBrandDeleted() {
            // Arrange
            Brand nike = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://nike.png"));
            Brand adidas = brandRepository.save(new Brand("Adidas", "독일 브랜드", "https://adidas.png"));
            Category category = categoryRepository.save(new Category("스포츠"));
            Product nikeProduct = productService.createProduct("나이키 신발", nike.getId(), category.getId(), 100000L);
            Product adidasProduct = productService.createProduct("아디다스 신발", adidas.getId(), category.getId(), 120000L);

            // Act
            brandService.deleteBrand(nike.getId());

            // Assert
            Product deletedNikeProduct = productRepository.findById(nikeProduct.getId()).orElseThrow();
            Product activeAdidasProduct = productRepository.findById(adidasProduct.getId()).orElseThrow();
            assertAll(
                () -> assertThat(deletedNikeProduct.isDeleted()).isTrue(),
                () -> assertThat(activeAdidasProduct.isDeleted()).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("validateBrand")
    class ValidateBrand {

        @Test
        @DisplayName("존재하고 활성인 브랜드를 검증하면 Brand를 반환한다")
        void returnsBrand_whenBrandIsValid() {
            // Arrange
            Brand saved = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));

            // Act
            Brand result = brandService.validateBrand(saved.getId());

            // Assert
            assertThat(result.getName()).isEqualTo("Nike");
        }

        @Test
        @DisplayName("삭제된 브랜드를 검증하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandIsDeleted() {
            // Arrange
            Brand saved = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));
            brandService.deleteBrand(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> brandService.validateBrand(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getActiveBrandsByIds")
    class GetActiveBrandsByIds {

        @Test
        @DisplayName("여러 브랜드 ID로 한 번에 조회한다")
        void returnsBrands_whenIdsProvided() {
            // Arrange
            Brand nike = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://nike.png"));
            Brand adidas = brandRepository.save(new Brand("Adidas", "독일 브랜드", "https://adidas.png"));
            Brand puma = brandRepository.save(new Brand("Puma", "유럽 브랜드", "https://puma.png"));
            List<Long> brandIds = List.of(nike.getId(), adidas.getId(), puma.getId());

            // Act
            Map<Long, Brand> result = brandService.getActiveBrandsByIds(brandIds);

            // Assert
            assertAll(
                () -> assertThat(result).hasSize(3),
                () -> assertThat(result.get(nike.getId()).getName()).isEqualTo("Nike"),
                () -> assertThat(result.get(adidas.getId()).getName()).isEqualTo("Adidas"),
                () -> assertThat(result.get(puma.getId()).getName()).isEqualTo("Puma")
            );
        }

        @Test
        @DisplayName("삭제된 브랜드는 결과에 포함되지 않는다")
        void excludesDeletedBrands() {
            // Arrange
            Brand nike = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://nike.png"));
            Brand adidas = brandRepository.save(new Brand("Adidas", "독일 브랜드", "https://adidas.png"));
            brandService.deleteBrand(adidas.getId());
            List<Long> brandIds = List.of(nike.getId(), adidas.getId());

            // Act
            Map<Long, Brand> result = brandService.getActiveBrandsByIds(brandIds);

            // Assert
            assertAll(
                () -> assertThat(result).hasSize(1),
                () -> assertThat(result.containsKey(nike.getId())).isTrue(),
                () -> assertThat(result.containsKey(adidas.getId())).isFalse()
            );
        }

        @Test
        @DisplayName("빈 ID 목록이면 빈 Map을 반환한다")
        void returnsEmptyMap_whenIdsIsEmpty() {
            // Arrange
            List<Long> emptyIds = List.of();

            // Act
            Map<Long, Brand> result = brandService.getActiveBrandsByIds(emptyIds);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("null ID 목록이면 빈 Map을 반환한다")
        void returnsEmptyMap_whenIdsIsNull() {
            // Act
            Map<Long, Brand> result = brandService.getActiveBrandsByIds(null);

            // Assert
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 ID는 결과에 포함되지 않는다")
        void excludesNonExistentIds() {
            // Arrange
            Brand nike = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://nike.png"));
            List<Long> brandIds = List.of(nike.getId(), 9999L);

            // Act
            Map<Long, Brand> result = brandService.getActiveBrandsByIds(brandIds);

            // Assert
            assertAll(
                () -> assertThat(result).hasSize(1),
                () -> assertThat(result.containsKey(nike.getId())).isTrue(),
                () -> assertThat(result.containsKey(9999L)).isFalse()
            );
        }
    }
}