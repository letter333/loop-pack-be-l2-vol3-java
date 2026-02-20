package com.loopers.application.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
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
@DisplayName("BrandFacade 통합 테스트")
class BrandFacadeTest {

    @Autowired
    private BrandFacade brandFacade;

    @Autowired
    private BrandService brandService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String VALID_ADMIN_LDAP = "loopers.admin";
    private static final String INVALID_ADMIN_LDAP = "invalid.ldap";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("getBrandInfo (사용자용)")
    class GetBrandInfo {

        @Test
        @DisplayName("활성 브랜드 정보를 조회하면 BrandInfo를 반환한다")
        void returnsBrandInfo_whenBrandIsActive() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act
            BrandInfo result = brandFacade.getBrandInfo(saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(saved.getId()),
                () -> assertThat(result.name()).isEqualTo("Nike"),
                () -> assertThat(result.description()).isEqualTo("스포츠 브랜드"),
                () -> assertThat(result.logoImageUrl()).isEqualTo("https://logo.png")
            );
        }

        @Test
        @DisplayName("삭제된 브랜드를 조회하면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandIsDeleted() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");
            brandService.deleteBrand(saved.getId());

            // Act & Assert
            assertThatThrownBy(() -> brandFacade.getBrandInfo(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getBrandInfos (사용자용 목록)")
    class GetBrandInfos {

        @Test
        @DisplayName("활성 브랜드 목록을 조회하면 BrandInfo 페이지를 반환한다")
        void returnsBrandInfoPage_whenBrandsExist() {
            // Arrange
            brandService.createBrand("Nike", "스포츠", "https://nike.png");
            brandService.createBrand("Adidas", "독일", "https://adidas.png");

            // Act
            Page<BrandInfo> result = brandFacade.getBrandInfos(PageRequest.of(0, 10));

            // Assert
            assertAll(
                () -> assertThat(result.getTotalElements()).isEqualTo(2),
                () -> assertThat(result.getContent()).extracting(BrandInfo::name)
                    .containsExactlyInAnyOrder("Nike", "Adidas")
            );
        }
    }

    @Nested
    @DisplayName("getBrandDetail (Admin용)")
    class GetBrandDetail {

        @Test
        @DisplayName("Admin이 브랜드 상세를 조회하면 BrandDetailInfo를 반환한다")
        void returnsBrandDetailInfo_whenAdminRequests() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act
            BrandDetailInfo result = brandFacade.getBrandDetail(VALID_ADMIN_LDAP, saved.getId());

            // Assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(saved.getId()),
                () -> assertThat(result.name()).isEqualTo("Nike"),
                () -> assertThat(result.createdAt()).isNotNull(),
                () -> assertThat(result.deletedAt()).isNull()
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 브랜드 상세를 조회하면 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNonAdminRequests() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act & Assert
            assertThatThrownBy(() -> brandFacade.getBrandDetail(INVALID_ADMIN_LDAP, saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("getBrandDetails (Admin용 목록)")
    class GetBrandDetails {

        @Test
        @DisplayName("Admin이 브랜드 목록을 조회하면 BrandDetailInfo 페이지를 반환한다")
        void returnsBrandDetailInfoPage_whenAdminRequests() {
            // Arrange
            brandService.createBrand("Nike", "스포츠", "https://nike.png");
            brandService.createBrand("Adidas", "독일", "https://adidas.png");

            // Act
            Page<BrandDetailInfo> result = brandFacade.getBrandDetails(VALID_ADMIN_LDAP, PageRequest.of(0, 10));

            // Assert
            assertAll(
                () -> assertThat(result.getTotalElements()).isEqualTo(2),
                () -> assertThat(result.getContent()).extracting(BrandDetailInfo::name)
                    .containsExactlyInAnyOrder("Nike", "Adidas")
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 브랜드 목록을 조회하면 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNonAdminRequests() {
            // Act & Assert
            assertThatThrownBy(() -> brandFacade.getBrandDetails(INVALID_ADMIN_LDAP, PageRequest.of(0, 10)))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("createBrand (Admin용)")
    class CreateBrand {

        @Test
        @DisplayName("Admin이 브랜드를 등록하면 BrandDetailInfo를 반환한다")
        void returnsBrandDetailInfo_whenAdminCreates() {
            // Act
            BrandDetailInfo result = brandFacade.createBrand(VALID_ADMIN_LDAP, "Nike", "스포츠 브랜드", "https://logo.png");

            // Assert
            assertAll(
                () -> assertThat(result.id()).isNotNull(),
                () -> assertThat(result.name()).isEqualTo("Nike"),
                () -> assertThat(result.createdAt()).isNotNull()
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 브랜드를 등록하면 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNonAdminCreates() {
            // Act & Assert
            assertThatThrownBy(() -> brandFacade.createBrand(INVALID_ADMIN_LDAP, "Nike", "설명", "https://logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("updateBrand (Admin용)")
    class UpdateBrand {

        @Test
        @DisplayName("Admin이 브랜드를 수정하면 BrandDetailInfo를 반환한다")
        void returnsBrandDetailInfo_whenAdminUpdates() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act
            BrandDetailInfo result = brandFacade.updateBrand(VALID_ADMIN_LDAP, saved.getId(), "Adidas", "독일 브랜드", "https://adidas.png");

            // Assert
            assertAll(
                () -> assertThat(result.name()).isEqualTo("Adidas"),
                () -> assertThat(result.description()).isEqualTo("독일 브랜드"),
                () -> assertThat(result.logoImageUrl()).isEqualTo("https://adidas.png")
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 브랜드를 수정하면 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNonAdminUpdates() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act & Assert
            assertThatThrownBy(() -> brandFacade.updateBrand(INVALID_ADMIN_LDAP, saved.getId(), "Adidas", "설명", "https://logo.png"))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }

    @Nested
    @DisplayName("deleteBrand (Admin용)")
    class DeleteBrand {

        @Test
        @DisplayName("Admin이 브랜드를 삭제하면 정상 처리된다")
        void deletesSuccessfully_whenAdminDeletes() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act
            brandFacade.deleteBrand(VALID_ADMIN_LDAP, saved.getId());

            // Assert
            assertThatThrownBy(() -> brandFacade.getBrandInfo(saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 브랜드를 삭제하면 FORBIDDEN 예외가 발생한다")
        void throwsForbidden_whenNonAdminDeletes() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://logo.png");

            // Act & Assert
            assertThatThrownBy(() -> brandFacade.deleteBrand(INVALID_ADMIN_LDAP, saved.getId()))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.FORBIDDEN));
        }
    }
}