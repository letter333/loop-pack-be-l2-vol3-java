package com.loopers.interfaces.api.category;

import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Category Admin V1 API E2E 테스트")
class CategoryAdminV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/admin/categories";
    private static final String VALID_ADMIN_LDAP = "loopers.admin";
    private static final String INVALID_ADMIN_LDAP = "invalid.ldap";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private HttpHeaders createAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-Ldap", VALID_ADMIN_LDAP);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders createInvalidAdminHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-Ldap", INVALID_ADMIN_LDAP);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    @Nested
    @DisplayName("POST /api/v1/admin/categories")
    class CreateCategory {

        @Test
        @DisplayName("Admin이 루트 카테고리를 등록하면 201 Created를 반환한다")
        void returnsCreated_whenAdminCreatesRootCategory() {
            // Arrange
            CategoryAdminV1Dto.CreateCategoryRequest request =
                new CategoryAdminV1Dto.CreateCategoryRequest("전자제품", null);

            // Act
            ParameterizedTypeReference<ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse>> response =
                testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, createAdminHeaders()),
                    responseType
                );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().id()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("전자제품"),
                () -> assertThat(response.getBody().data().parentId()).isNull(),
                () -> assertThat(response.getBody().data().depth()).isEqualTo(0)
            );
        }

        @Test
        @DisplayName("Admin이 하위 카테고리를 등록하면 201 Created를 반환한다")
        void returnsCreated_whenAdminCreatesChildCategory() {
            // Arrange
            Category parent = categoryService.createCategory("전자제품", null);
            CategoryAdminV1Dto.CreateCategoryRequest request =
                new CategoryAdminV1Dto.CreateCategoryRequest("휴대폰", parent.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse>> response =
                testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, createAdminHeaders()),
                    responseType
                );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().parentId()).isEqualTo(parent.getId()),
                () -> assertThat(response.getBody().data().depth()).isEqualTo(1)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 등록하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminCreates() {
            // Arrange
            CategoryAdminV1Dto.CreateCategoryRequest request =
                new CategoryAdminV1Dto.CreateCategoryRequest("전자제품", null);

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, createInvalidAdminHeaders()),
                    responseType
                );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/categories/{categoryId}")
    class UpdateCategory {

        @Test
        @DisplayName("Admin이 카테고리를 수정하면 200 OK를 반환한다")
        void returnsOk_whenAdminUpdates() {
            // Arrange
            Category saved = categoryService.createCategory("전자제품", null);
            CategoryAdminV1Dto.UpdateCategoryRequest request =
                new CategoryAdminV1Dto.UpdateCategoryRequest("가전제품");

            // Act
            ParameterizedTypeReference<ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<CategoryAdminV1Dto.CategoryDetailResponse>> response =
                testRestTemplate.exchange(
                    ENDPOINT + "/" + saved.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(request, createAdminHeaders()),
                    responseType
                );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().name()).isEqualTo("가전제품")
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 수정하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminUpdates() {
            // Arrange
            Category saved = categoryService.createCategory("전자제품", null);
            CategoryAdminV1Dto.UpdateCategoryRequest request =
                new CategoryAdminV1Dto.UpdateCategoryRequest("가전제품");

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                    ENDPOINT + "/" + saved.getId(),
                    HttpMethod.PUT,
                    new HttpEntity<>(request, createInvalidAdminHeaders()),
                    responseType
                );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/categories/{categoryId}")
    class DeleteCategory {

        @Test
        @DisplayName("Admin이 카테고리를 삭제하면 200 OK를 반환한다")
        void returnsOk_whenAdminDeletes() {
            // Arrange
            Category saved = categoryService.createCategory("전자제품", null);

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                    ENDPOINT + "/" + saved.getId(),
                    HttpMethod.DELETE,
                    new HttpEntity<>(createAdminHeaders()),
                    responseType
                );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 삭제하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminDeletes() {
            // Arrange
            Category saved = categoryService.createCategory("전자제품", null);

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(
                    ENDPOINT + "/" + saved.getId(),
                    HttpMethod.DELETE,
                    new HttpEntity<>(createInvalidAdminHeaders()),
                    responseType
                );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("하위 카테고리도 함께 삭제된다")
        void deletesChildCategories() {
            // Arrange
            Category parent = categoryService.createCategory("전자제품", null);
            Category child = categoryService.createCategory("휴대폰", parent.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};
            testRestTemplate.exchange(
                ENDPOINT + "/" + parent.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            Category deletedParent = categoryService.getCategory(parent.getId());
            Category deletedChild = categoryService.getCategory(child.getId());
            assertAll(
                () -> assertThat(deletedParent.isDeleted()).isTrue(),
                () -> assertThat(deletedChild.isDeleted()).isTrue()
            );
        }
    }
}