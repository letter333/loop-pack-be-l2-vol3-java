package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryRepository;
import com.loopers.domain.product.DiscountType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
@DisplayName("Product Admin V1 API E2E 테스트")
class ProductAdminV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/admin/products";
    private static final String VALID_ADMIN_LDAP = "loopers.admin";
    private static final String INVALID_ADMIN_LDAP = "invalid.ldap";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Brand savedBrand;
    private Category savedCategory;

    @BeforeEach
    void setUp() {
        savedBrand = brandRepository.save(new Brand("Apple", "애플", "https://example.com/apple.png"));
        savedCategory = categoryRepository.save(new Category("전자제품"));
    }

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
    @DisplayName("GET /api/v1/admin/products")
    class GetProducts {

        @Test
        @DisplayName("Admin이 상품 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));

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

        @Test
        @DisplayName("삭제된 상품도 목록에 포함된다")
        void includesDeletedProducts() {
            // Arrange
            Product activeProduct = productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            Product toDelete = productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));
            productService.deleteProduct(toDelete.getId());

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
        @DisplayName("삭제된 상품은 deletedAt 필드가 포함되어 반환된다")
        void returnsDeletedAtField_whenProductIsDeleted() {
            // Arrange
            Product toDelete = productRepository.save(new Product("삭제될 상품", savedBrand.getId(), 1L, 1000000L));
            productService.deleteProduct(toDelete.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().data().get("content");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(content).hasSize(1),
                () -> assertThat(content.get(0).get("deletedAt")).isNotNull()
            );
        }

        @Test
        @DisplayName("페이징이 정상적으로 동작한다")
        void returnsPaginatedProducts() {
            // Arrange
            for (int i = 0; i < 15; i++) {
                productRepository.save(new Product("상품" + i, savedBrand.getId(), 1L, 1000000L + i));
            }

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
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(10),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(15),
                () -> assertThat(response.getBody().data().get("totalPages")).isEqualTo(2)
            );
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/products/{productId}")
    class GetProduct {

        @Test
        @DisplayName("Admin이 상품 상세를 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("아이폰 15"),
                () -> assertThat(response.getBody().data().get("basePrice")).isEqualTo(1500000)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 조회하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminRequests() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 조회하면 404 Not Found를 반환한다")
        void returnsNotFound_whenProductNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("삭제된 상품도 조회할 수 있다")
        void returnsDeletedProduct() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));
            productService.deleteProduct(product.getId());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("deletedAt")).isNotNull()
            );
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/products")
    class CreateProduct {

        @Test
        @DisplayName("Admin이 상품을 등록하면 201 Created를 반환한다")
        void returnsCreated_whenAdminCreates() {
            // Arrange
            String requestBody = """
                {
                    "name": "새 상품",
                    "brandId": %d,
                    "categoryId": %d,
                    "basePrice": 50000
                }
                """.formatted(savedBrand.getId(), savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                request,
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("새 상품"),
                () -> assertThat(response.getBody().data().get("basePrice")).isEqualTo(50000)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 등록하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminCreates() {
            // Arrange
            String requestBody = """
                {
                    "name": "새 상품",
                    "brandId": %d,
                    "categoryId": %d,
                    "basePrice": 50000
                }
                """.formatted(savedBrand.getId(), savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createInvalidAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("필수 필드가 누락되면 400 Bad Request를 반환한다")
        void returnsBadRequest_whenRequiredFieldMissing() {
            // Arrange - name 누락
            String requestBody = """
                {
                    "brandId": %d,
                    "categoryId": %d,
                    "basePrice": 50000
                }
                """.formatted(savedBrand.getId(), savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("존재하지 않는 브랜드 ID로 등록하면 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandNotExists() {
            // Arrange
            String requestBody = """
                {
                    "name": "새 상품",
                    "brandId": 99999,
                    "categoryId": %d,
                    "basePrice": 50000
                }
                """.formatted(savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("존재하지 않는 카테고리 ID로 등록하면 404 Not Found를 반환한다")
        void returnsNotFound_whenCategoryNotExists() {
            // Arrange
            String requestBody = """
                {
                    "name": "새 상품",
                    "brandId": %d,
                    "categoryId": 99999,
                    "basePrice": 50000
                }
                """.formatted(savedBrand.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/products/{productId}")
    class UpdateProduct {

        @Test
        @DisplayName("Admin이 상품을 수정하면 200 OK를 반환한다")
        void returnsOk_whenAdminUpdates() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));
            String requestBody = """
                {
                    "name": "아이폰 15 Pro",
                    "categoryId": %d,
                    "basePrice": 1800000,
                    "status": "SALE"
                }
                """.formatted(savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("아이폰 15 Pro"),
                () -> assertThat(response.getBody().data().get("basePrice")).isEqualTo(1800000)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 수정하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminUpdates() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));
            String requestBody = """
                {
                    "name": "아이폰 15 Pro",
                    "categoryId": %d,
                    "basePrice": 1800000,
                    "status": "SALE"
                }
                """.formatted(savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createInvalidAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하면 404 Not Found를 반환한다")
        void returnsNotFound_whenProductNotExists() {
            // Arrange
            String requestBody = """
                {
                    "name": "아이폰 15 Pro",
                    "categoryId": %d,
                    "basePrice": 1800000,
                    "status": "SALE"
                }
                """.formatted(savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999",
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("상태를 STOP으로 변경할 수 있다")
        void updatesStatusToStop() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));
            String requestBody = """
                {
                    "name": "아이폰 15",
                    "categoryId": %d,
                    "basePrice": 1500000,
                    "status": "STOP"
                }
                """.formatted(savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("status")).isEqualTo("STOP")
            );
        }

        @Test
        @DisplayName("할인 정보를 설정할 수 있다")
        void updatesDiscountInfo() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));
            String requestBody = """
                {
                    "name": "아이폰 15",
                    "categoryId": %d,
                    "basePrice": 1500000,
                    "discount": 10,
                    "discountType": "RATE",
                    "status": "SALE"
                }
                """.formatted(savedCategory.getId());

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("discount")).isEqualTo(10),
                () -> assertThat(response.getBody().data().get("discountType")).isEqualTo("RATE"),
                () -> assertThat(response.getBody().data().get("discountedPrice")).isEqualTo(1350000)
            );
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/products/{productId}")
    class DeleteProduct {

        @Test
        @DisplayName("Admin이 상품을 삭제하면 200 OK를 반환한다")
        void returnsOk_whenAdminDeletes() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("삭제 후 상품 조회 시 deletedAt이 설정된다")
        void setsDeletedAt_afterDeletion() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));

            // Act
            testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            // Assert - 삭제된 상품 조회
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> getResponse = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            assertAll(
                () -> assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(getResponse.getBody().data().get("deletedAt")).isNotNull()
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 삭제하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminDeletes() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), savedCategory.getId(), 1500000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + product.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하면 404 Not Found를 반환한다")
        void returnsNotFound_whenProductNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999",
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
