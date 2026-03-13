package com.loopers.interfaces.api.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Product V1 API E2E 테스트")
class ProductV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/products";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Brand savedBrand;
    private Brand savedBrand2;

    @BeforeEach
    void setUp() {
        savedBrand = brandRepository.save(new Brand("Apple", "애플", "https://example.com/apple.png"));
        savedBrand2 = brandRepository.save(new Brand("Samsung", "삼성", "https://example.com/samsung.png"));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class GetProducts {

        @Test
        @DisplayName("상품 목록을 페이지로 조회한다")
        void returnsPagedProducts() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("content")).isInstanceOf(List.class),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(3),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(3)
            );
        }

        @Test
        @DisplayName("카테고리 ID로 필터링하여 조회한다")
        void returnsFilteredProductsByCategoryId() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?categoryId=1", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(2),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(2)
            );
        }

        @Test
        @DisplayName("키워드로 검색하여 조회한다")
        void returnsFilteredProductsByKeyword() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            productRepository.save(new Product("아이폰 15 Pro", savedBrand.getId(), 1L, 1800000L));

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?keyword=아이폰", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(2),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(2)
            );
        }

        @Test
        @DisplayName("가격 낮은순으로 정렬하여 조회한다")
        void returnsProductsSortedByPriceAsc() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?sort=PRICE_ASC", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().data().get("content");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(content.get(0).get("basePrice")).isEqualTo(1400000),
                () -> assertThat(content.get(1).get("basePrice")).isEqualTo(1500000),
                () -> assertThat(content.get(2).get("basePrice")).isEqualTo(3000000)
            );
        }

        @Test
        @DisplayName("페이징 파라미터가 정상적으로 동작한다")
        void returnsPaginatedProducts() {
            // Arrange
            for (int i = 0; i < 25; i++) {
                productRepository.save(new Product("상품" + i, savedBrand.getId(), 1L, 1000000L + i));
            }

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?page=0&size=10", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(10),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(25),
                () -> assertThat(response.getBody().data().get("totalPages")).isEqualTo(3)
            );
        }

        @Test
        @DisplayName("기본 페이지 사이즈는 20이다")
        void returnsDefaultPageSize() {
            // Arrange
            for (int i = 0; i < 25; i++) {
                productRepository.save(new Product("상품" + i, savedBrand.getId(), 1L, 1000000L + i));
            }

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(20),
                () -> assertThat(response.getBody().data().get("size")).isEqualTo(20)
            );
        }

        @Test
        @DisplayName("삭제된 상품은 목록에서 제외된다")
        void excludesDeletedProducts() {
            // Arrange
            Product activeProduct = productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            Product toDelete = productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            productService.deleteProduct(toDelete.getId());

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(1),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(1)
            );
        }

        @Test
        @DisplayName("브랜드 정보가 포함되어 조회된다")
        void returnsProductsWithBrandInfo() {
            // Arrange
            productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().data().get("content");
            @SuppressWarnings("unchecked")
            Map<String, Object> brand = (Map<String, Object>) content.get(0).get("brand");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(brand).isNotNull(),
                () -> assertThat(brand.get("name")).isEqualTo("Apple")
            );
        }

        @Test
        @DisplayName("좋아요 많은순으로 정렬하여 조회한다")
        void returnsProductsSortedByLikesDesc() {
            // Arrange
            Product product1 = productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            Product product2 = productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            Product product3 = productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));

            // 좋아요 수 설정: product2(5) > product1(3) > product3(1)
            for (int i = 0; i < 3; i++) productService.increaseLikeCount(product1.getId());
            for (int i = 0; i < 5; i++) productService.increaseLikeCount(product2.getId());
            productService.increaseLikeCount(product3.getId());

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?sort=LIKES_DESC", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().data().get("content");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(content).hasSize(3),
                () -> assertThat(((Number) content.get(0).get("likeCount")).longValue()).isEqualTo(5L),
                () -> assertThat(((Number) content.get(1).get("likeCount")).longValue()).isEqualTo(3L),
                () -> assertThat(((Number) content.get(2).get("likeCount")).longValue()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("좋아요 수가 동일하면 최신순으로 정렬한다")
        void returnsProductsSortedByCreatedAtDesc_whenLikeCountSame() {
            // Arrange
            Product product1 = productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));
            Product product2 = productRepository.save(new Product("갤럭시 S24", savedBrand2.getId(), 1L, 1400000L));
            Product product3 = productRepository.save(new Product("맥북 프로", savedBrand.getId(), 2L, 3000000L));

            // 모든 상품 좋아요 수 동일하게 설정 (각 1개)
            productService.increaseLikeCount(product1.getId());
            productService.increaseLikeCount(product2.getId());
            productService.increaseLikeCount(product3.getId());

            // Act
            ResponseEntity<ApiResponse<Map<String, Object>>> response =
                testRestTemplate.exchange(ENDPOINT + "?sort=LIKES_DESC", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert - 좋아요 수 동일하면 최신순 (product3 > product2 > product1)
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().data().get("content");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(content).hasSize(3),
                () -> assertThat(content.get(0).get("name")).isEqualTo("맥북 프로"),
                () -> assertThat(content.get(1).get("name")).isEqualTo("갤럭시 S24"),
                () -> assertThat(content.get(2).get("name")).isEqualTo("아이폰 15")
            );
        }
    }

    @Nested
    @DisplayName("GET /api/v1/products/{productId}")
    class GetProduct {

        @Test
        @DisplayName("상품 상세 정보를 조회한다")
        void returnsProductDetail() {
            // Arrange
            Product product = productRepository.save(new Product("아이폰 15", savedBrand.getId(), 1L, 1500000L));

            // Act
            ResponseEntity<ApiResponse<ProductV1Dto.ProductDetailResponse>> response =
                testRestTemplate.exchange(ENDPOINT + "/" + product.getId(), HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(product.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo("아이폰 15"),
                () -> assertThat(response.getBody().data().brand().name()).isEqualTo("Apple"),
                () -> assertThat(response.getBody().data().options()).isEmpty(),
                () -> assertThat(response.getBody().data().images()).isEmpty()
            );
        }

        @Test
        @DisplayName("존재하지 않는 상품 조회 시 404 응답을 반환한다")
        void returns404_whenProductNotFound() {
            // Act
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(ENDPOINT + "/999", HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {});

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
