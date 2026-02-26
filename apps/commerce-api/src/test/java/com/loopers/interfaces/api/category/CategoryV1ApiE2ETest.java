package com.loopers.interfaces.api.category;

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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Category V1 API E2E 테스트")
class CategoryV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/categories";

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

    @Nested
    @DisplayName("GET /api/v1/categories")
    class GetCategories {

        @Test
        @DisplayName("카테고리 목록을 계층 구조로 조회한다")
        void returnsHierarchicalCategories() {
            // Arrange
            var parent = categoryService.createCategory("전자제품", null);
            categoryService.createCategory("휴대폰", parent.getId());
            categoryService.createCategory("의류", null);

            // Act
            ParameterizedTypeReference<ApiResponse<List<CategoryV1Dto.CategoryResponse>>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<List<CategoryV1Dto.CategoryResponse>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, responseType);

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(2),
                () -> assertThat(response.getBody().data())
                    .extracting(CategoryV1Dto.CategoryResponse::name)
                    .containsExactlyInAnyOrder("전자제품", "의류")
            );
        }

        @Test
        @DisplayName("삭제된 카테고리는 목록에서 제외된다")
        void excludesDeletedCategories() {
            // Arrange
            var category1 = categoryService.createCategory("전자제품", null);
            var category2 = categoryService.createCategory("의류", null);
            categoryService.deleteCategory(category2.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<List<CategoryV1Dto.CategoryResponse>>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<List<CategoryV1Dto.CategoryResponse>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, responseType);

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(1),
                () -> assertThat(response.getBody().data().get(0).name()).isEqualTo("전자제품")
            );
        }

        @Test
        @DisplayName("하위 카테고리가 children에 포함된다")
        void includesChildrenInHierarchy() {
            // Arrange
            var parent = categoryService.createCategory("전자제품", null);
            categoryService.createCategory("휴대폰", parent.getId());
            categoryService.createCategory("노트북", parent.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<List<CategoryV1Dto.CategoryResponse>>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<List<CategoryV1Dto.CategoryResponse>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, responseType);

            // Assert
            var rootCategory = response.getBody().data().get(0);
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(rootCategory.name()).isEqualTo("전자제품"),
                () -> assertThat(rootCategory.children()).hasSize(2),
                () -> assertThat(rootCategory.children())
                    .extracting(CategoryV1Dto.CategoryResponse::name)
                    .containsExactlyInAnyOrder("휴대폰", "노트북")
            );
        }
    }
}