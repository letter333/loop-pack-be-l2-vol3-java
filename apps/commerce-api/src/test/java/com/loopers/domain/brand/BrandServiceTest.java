package com.loopers.domain.brand;

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
}