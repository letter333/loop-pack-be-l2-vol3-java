package com.loopers.domain.category;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("CategoryService 통합 테스트")
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getCategory")
    class GetCategory {

        @Test
        @DisplayName("존재하는 카테고리를 조회하면 Category를 반환한다")
        void returnsCategory_whenCategoryExists() {
            // Arrange
            Category saved = categoryRepository.save(new Category("전자제품"));

            // Act
            Category result = categoryService.getCategory(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isEqualTo(saved.getId()),
                () -> assertThat(result.getName()).isEqualTo("전자제품")
            );
        }

        @Test
        @DisplayName("존재하지 않는 카테고리를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenCategoryNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> categoryService.getCategory(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getActiveCategory")
    class GetActiveCategory {

        @Test
        @DisplayName("활성 카테고리를 조회하면 Category를 반환한다")
        void returnsCategory_whenCategoryIsActive() {
            // Arrange
            Category saved = categoryRepository.save(new Category("전자제품"));

            // Act
            Category result = categoryService.getActiveCategory(saved.getId());

            // Assert
            assertThat(result.getName()).isEqualTo("전자제품");
        }

        @Test
        @DisplayName("삭제된 카테고리를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenCategoryIsDeleted() {
            // Arrange
            Category category = new Category("전자제품");
            Category saved = categoryRepository.save(category);
            categoryService.deleteCategory(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.getActiveCategory(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getAllActiveCategories")
    class GetAllActiveCategories {

        @Test
        @DisplayName("삭제되지 않은 카테고리만 조회한다")
        void returnsOnlyActiveCategories() {
            // Arrange
            categoryRepository.save(new Category("전자제품"));
            categoryRepository.save(new Category("의류"));
            Category toDelete = categoryRepository.save(new Category("식품"));
            categoryService.deleteCategory(toDelete.getId());

            // Act
            List<Category> result = categoryService.getAllActiveCategories();

            // Assert
            assertAll(
                () -> assertThat(result).hasSize(2),
                () -> assertThat(result).extracting(Category::getName)
                    .containsExactlyInAnyOrder("전자제품", "의류")
            );
        }
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("루트 카테고리를 정상적으로 생성한다")
        void createsRootCategory() {
            // Act
            Category result = categoryService.createCategory("전자제품", null);

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("전자제품"),
                () -> assertThat(result.isRoot()).isTrue(),
                () -> assertThat(result.getDepth()).isEqualTo(0),
                () -> assertThat(result.getPath()).isEqualTo(String.valueOf(result.getId()))
            );
        }

        @Test
        @DisplayName("하위 카테고리를 정상적으로 생성한다")
        void createsChildCategory() {
            // Arrange
            Category parent = categoryService.createCategory("전자제품", null);

            // Act
            Category result = categoryService.createCategory("휴대폰", parent.getId());

            // Assert
            assertAll(
                () -> assertThat(result.getId()).isNotNull(),
                () -> assertThat(result.getName()).isEqualTo("휴대폰"),
                () -> assertThat(result.getParentId()).isEqualTo(parent.getId()),
                () -> assertThat(result.getDepth()).isEqualTo(1),
                () -> assertThat(result.getPath()).isEqualTo(parent.getPath() + "/" + result.getId())
            );
        }

        @Test
        @DisplayName("존재하지 않는 부모 카테고리로 생성하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenParentNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> categoryService.createCategory("휴대폰", 999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("카테고리를 삭제하면 Soft Delete 된다")
        void deletesCategory() {
            // Arrange
            Category saved = categoryRepository.save(new Category("전자제품"));

            // Act
            categoryService.deleteCategory(saved.getId());

            // Assert
            Category deleted = categoryService.getCategory(saved.getId());
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 카테고리를 삭제하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenCategoryNotExists() {
            // Act & Assert
            assertThatThrownBy(() -> categoryService.deleteCategory(999L))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("카테고리 삭제 시 하위 카테고리도 함께 삭제된다")
        void deletesChildCategories_whenParentDeleted() {
            // Arrange
            Category parent = categoryService.createCategory("전자제품", null);
            Category child = categoryService.createCategory("휴대폰", parent.getId());
            Category grandChild = categoryService.createCategory("스마트폰", child.getId());

            // Act
            categoryService.deleteCategory(parent.getId());

            // Assert
            assertAll(
                () -> assertThat(categoryService.getCategory(parent.getId()).isDeleted()).isTrue(),
                () -> assertThat(categoryService.getCategory(child.getId()).isDeleted()).isTrue(),
                () -> assertThat(categoryService.getCategory(grandChild.getId()).isDeleted()).isTrue()
            );
        }

        @Test
        @DisplayName("카테고리 삭제 시 연관된 상품도 함께 Soft Delete 된다")
        void deletesRelatedProducts_whenCategoryDeleted() {
            // Arrange
            Brand brand = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));
            Category category = categoryService.createCategory("스포츠", null);
            Product product1 = productService.createProduct("나이키 신발", brand.getId(), category.getId(), 100000L);
            Product product2 = productService.createProduct("나이키 가방", brand.getId(), category.getId(), 50000L);

            // Act
            categoryService.deleteCategory(category.getId());

            // Assert
            Product deletedProduct1 = productRepository.findById(product1.getId()).orElseThrow();
            Product deletedProduct2 = productRepository.findById(product2.getId()).orElseThrow();
            assertAll(
                () -> assertThat(deletedProduct1.isDeleted()).isTrue(),
                () -> assertThat(deletedProduct2.isDeleted()).isTrue()
            );
        }

        @Test
        @DisplayName("부모 카테고리 삭제 시 하위 카테고리의 상품도 함께 Soft Delete 된다")
        void deletesChildCategoryProducts_whenParentCategoryDeleted() {
            // Arrange
            Brand brand = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));
            Category parent = categoryService.createCategory("전자제품", null);
            Category child = categoryService.createCategory("휴대폰", parent.getId());
            Category grandChild = categoryService.createCategory("스마트폰", child.getId());
            Product parentProduct = productService.createProduct("전자제품1", brand.getId(), parent.getId(), 100000L);
            Product childProduct = productService.createProduct("휴대폰1", brand.getId(), child.getId(), 200000L);
            Product grandChildProduct = productService.createProduct("스마트폰1", brand.getId(), grandChild.getId(), 300000L);

            // Act
            categoryService.deleteCategory(parent.getId());

            // Assert
            Product deletedParentProduct = productRepository.findById(parentProduct.getId()).orElseThrow();
            Product deletedChildProduct = productRepository.findById(childProduct.getId()).orElseThrow();
            Product deletedGrandChildProduct = productRepository.findById(grandChildProduct.getId()).orElseThrow();
            assertAll(
                () -> assertThat(deletedParentProduct.isDeleted()).isTrue(),
                () -> assertThat(deletedChildProduct.isDeleted()).isTrue(),
                () -> assertThat(deletedGrandChildProduct.isDeleted()).isTrue()
            );
        }

        @Test
        @DisplayName("연관 상품이 없는 카테고리 삭제 시 정상 동작한다")
        void deletesSuccessfully_whenNoRelatedProducts() {
            // Arrange
            Category category = categoryService.createCategory("전자제품", null);

            // Act
            categoryService.deleteCategory(category.getId());

            // Assert
            Category deleted = categoryService.getCategory(category.getId());
            assertThat(deleted.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("다른 카테고리의 상품은 영향받지 않는다")
        void doesNotAffectOtherCategoryProducts_whenCategoryDeleted() {
            // Arrange
            Brand brand = brandRepository.save(new Brand("Nike", "스포츠 브랜드", "https://logo.png"));
            Category sports = categoryService.createCategory("스포츠", null);
            Category electronics = categoryService.createCategory("전자제품", null);
            Product sportsProduct = productService.createProduct("스포츠용품", brand.getId(), sports.getId(), 100000L);
            Product electronicsProduct = productService.createProduct("전자제품1", brand.getId(), electronics.getId(), 200000L);

            // Act
            categoryService.deleteCategory(sports.getId());

            // Assert
            Product deletedSportsProduct = productRepository.findById(sportsProduct.getId()).orElseThrow();
            Product activeElectronicsProduct = productRepository.findById(electronicsProduct.getId()).orElseThrow();
            assertAll(
                () -> assertThat(deletedSportsProduct.isDeleted()).isTrue(),
                () -> assertThat(activeElectronicsProduct.isDeleted()).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("validateCategory")
    class ValidateCategory {

        @Test
        @DisplayName("존재하고 활성인 카테고리를 검증하면 Category를 반환한다")
        void returnsCategory_whenCategoryIsValid() {
            // Arrange
            Category saved = categoryRepository.save(new Category("전자제품"));

            // Act
            Category result = categoryService.validateCategory(saved.getId());

            // Assert
            assertThat(result.getName()).isEqualTo("전자제품");
        }

        @Test
        @DisplayName("삭제된 카테고리를 검증하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenCategoryIsDeleted() {
            // Arrange
            Category saved = categoryRepository.save(new Category("전자제품"));
            categoryService.deleteCategory(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> categoryService.validateCategory(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }
}