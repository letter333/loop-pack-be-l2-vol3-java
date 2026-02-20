package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.brand.Brand;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Brand V1 API E2E 테스트")
class BrandV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/brands";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandService brandService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("GET /api/v1/brands")
    class GetBrands {

        @Test
        @DisplayName("브랜드 목록을 조회하면 200 OK와 페이징된 목록을 반환한다")
        void returnsOk_whenGetBrands() {
            // Arrange
            brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");
            brandService.createBrand("Adidas", "독일 브랜드", "https://adidas.png");

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?page=0&size=10", HttpMethod.GET, null, responseType);

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(2),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(2)
            );
        }

        @Test
        @DisplayName("삭제된 브랜드는 목록에서 제외된다")
        void excludesDeletedBrands() {
            // Arrange
            brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");
            Brand toDelete = brandService.createBrand("Adidas", "독일 브랜드", "https://adidas.png");
            brandService.deleteBrand(toDelete.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?page=0&size=10", HttpMethod.GET, null, responseType);

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(1)
            );
        }

        @Test
        @DisplayName("페이징이 정상 동작한다")
        void returnsPaginatedResults() {
            // Arrange
            for (int i = 1; i <= 15; i++) {
                brandService.createBrand("Brand" + i, "설명" + i, "https://logo" + i + ".png");
            }

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?page=1&size=10", HttpMethod.GET, null, responseType);

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(15),
                () -> assertThat(response.getBody().data().get("totalPages")).isEqualTo(2),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(5)
            );
        }
    }

    @Nested
    @DisplayName("GET /api/v1/brands/{brandId}")
    class GetBrand {

        @Test
        @DisplayName("존재하는 브랜드를 조회하면 200 OK와 브랜드 정보를 반환한다")
        void returnsOk_whenBrandExists() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");

            // Act
            ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                testRestTemplate.exchange(ENDPOINT + "/" + saved.getId(), HttpMethod.GET, null, responseType);

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(saved.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike"),
                () -> assertThat(response.getBody().data().description()).isEqualTo("스포츠 브랜드"),
                () -> assertThat(response.getBody().data().logoImageUrl()).isEqualTo("https://nike.png")
            );
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(ENDPOINT + "/999", HttpMethod.GET, null, responseType);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("삭제된 브랜드를 조회하면 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandIsDeleted() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");
            brandService.deleteBrand(saved.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(ENDPOINT + "/" + saved.getId(), HttpMethod.GET, null, responseType);

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}