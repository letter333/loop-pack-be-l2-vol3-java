package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Brand Admin V1 API E2E 테스트")
class BrandAdminV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/admin/brands";
    private static final String VALID_ADMIN_LDAP = "loopers.admin";
    private static final String INVALID_ADMIN_LDAP = "invalid.ldap";

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
    @DisplayName("GET /api/v1/admin/brands")
    class GetBrands {

        @Test
        @DisplayName("Admin이 브랜드 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");
            brandService.createBrand("Adidas", "독일 브랜드", "https://adidas.png");

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(2)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 조회하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminRequests() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/brands/{brandId}")
    class GetBrand {

        @Test
        @DisplayName("Admin이 브랜드 상세를 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");

            // Act
            ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandDetailResponse>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + saved.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(saved.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike"),
                () -> assertThat(response.getBody().data().createdAt()).isNotNull()
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 조회하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminRequests() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + saved.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 조회하면 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/999",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/brands")
    class CreateBrand {

        @Test
        @DisplayName("Admin이 브랜드를 등록하면 201 Created를 반환한다")
        void returnsCreated_whenAdminCreates() {
            // Arrange
            BrandAdminV1Dto.CreateBrandRequest request = new BrandAdminV1Dto.CreateBrandRequest(
                "Nike", "스포츠 브랜드", "https://nike.png"
            );

            // Act
            ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandDetailResponse>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().id()).isNotNull(),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Nike")
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 등록하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminCreates() {
            // Arrange
            BrandAdminV1Dto.CreateBrandRequest request = new BrandAdminV1Dto.CreateBrandRequest(
                "Nike", "스포츠 브랜드", "https://nike.png"
            );

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("필수 필드가 누락되면 400 Bad Request를 반환한다")
        void returnsBadRequest_whenNameMissing() {
            // Arrange
            BrandAdminV1Dto.CreateBrandRequest request = new BrandAdminV1Dto.CreateBrandRequest(
                null, "스포츠 브랜드", "https://nike.png"
            );

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(request, createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/brands/{brandId}")
    class UpdateBrand {

        @Test
        @DisplayName("Admin이 브랜드를 수정하면 200 OK를 반환한다")
        void returnsOk_whenAdminUpdates() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");
            BrandAdminV1Dto.UpdateBrandRequest request = new BrandAdminV1Dto.UpdateBrandRequest(
                "Adidas", "독일 브랜드", "https://adidas.png"
            );

            // Act
            ParameterizedTypeReference<ApiResponse<BrandAdminV1Dto.BrandDetailResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandAdminV1Dto.BrandDetailResponse>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + saved.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().name()).isEqualTo("Adidas"),
                () -> assertThat(response.getBody().data().description()).isEqualTo("독일 브랜드")
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 수정하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminUpdates() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");
            BrandAdminV1Dto.UpdateBrandRequest request = new BrandAdminV1Dto.UpdateBrandRequest(
                "Adidas", "독일 브랜드", "https://adidas.png"
            );

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + saved.getId(),
                HttpMethod.PUT,
                new HttpEntity<>(request, createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 수정하면 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandNotExists() {
            // Arrange
            BrandAdminV1Dto.UpdateBrandRequest request = new BrandAdminV1Dto.UpdateBrandRequest(
                "Adidas", "독일 브랜드", "https://adidas.png"
            );

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/999",
                HttpMethod.PUT,
                new HttpEntity<>(request, createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/brands/{brandId}")
    class DeleteBrand {

        @Test
        @DisplayName("Admin이 브랜드를 삭제하면 200 OK를 반환한다")
        void returnsOk_whenAdminDeletes() {
            // Arrange
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
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
            Brand saved = brandService.createBrand("Nike", "스포츠 브랜드", "https://nike.png");

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + saved.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 브랜드를 삭제하면 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/999",
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}