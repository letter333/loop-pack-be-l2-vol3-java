package com.loopers.interfaces.api.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public LikeV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        MemberRepository memberRepository,
        ProductRepository productRepository,
        BrandRepository brandRepository,
        PasswordEncoder passwordEncoder,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberRepository = memberRepository;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.passwordEncoder = passwordEncoder;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/products/{productId}/like")
    @Nested
    class ToggleProductLike {

        private Member member;
        private Brand brand;
        private Product product;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            brand = saveBrand("Nike");
            product = saveProduct("Test Product", brand.getId());
        }

        @Test
        @DisplayName("좋아요 추가 시 200 OK와 liked=true, likeCount=1을 반환한다")
        void returnsOkAndLikedTrue_whenLikeAdded() {
            // act
            ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response = toggleProductLike(
                product.getId(), member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().liked()).isTrue(),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("좋아요 토글 시 두 번째 호출에서 liked=false, likeCount=0을 반환한다")
        void returnsLikedFalse_whenToggledTwice() {
            // arrange - 첫 번째 좋아요
            toggleProductLike(product.getId(), member.getLoginId(), "Password123!");

            // act - 두 번째 좋아요 (취소)
            ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response = toggleProductLike(
                product.getId(), member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().liked()).isFalse(),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(0L)
            );
        }

        @Test
        @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenAuthenticationFails() {
            // act
            ResponseEntity<ApiResponse<Object>> response = toggleProductLikeWithError(
                product.getId(), member.getLoginId(), "WrongPassword!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("존재하지 않는 상품에 좋아요 시 404 Not Found를 반환한다")
        void returnsNotFound_whenProductNotExists() {
            // act
            ResponseEntity<ApiResponse<Object>> response = toggleProductLikeWithError(
                999L, member.getLoginId(), "Password123!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        private ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> toggleProductLike(
            Long productId, String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>> responseType =
                new ParameterizedTypeReference<>() {};

            return testRestTemplate.exchange(
                "/api/v1/products/" + productId + "/like",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType
            );
        }

        private ResponseEntity<ApiResponse<Object>> toggleProductLikeWithError(
            Long productId, String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};

            return testRestTemplate.exchange(
                "/api/v1/products/" + productId + "/like",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType
            );
        }
    }

    @DisplayName("POST /api/v1/brands/{brandId}/like")
    @Nested
    class ToggleBrandLike {

        private Member member;
        private Brand brand;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            brand = saveBrand("Nike");
        }

        @Test
        @DisplayName("좋아요 추가 시 200 OK와 liked=true, likeCount=1을 반환한다")
        void returnsOkAndLikedTrue_whenLikeAdded() {
            // act
            ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response = toggleBrandLike(
                brand.getId(), member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().liked()).isTrue(),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("좋아요 토글 시 두 번째 호출에서 liked=false, likeCount=0을 반환한다")
        void returnsLikedFalse_whenToggledTwice() {
            // arrange - 첫 번째 좋아요
            toggleBrandLike(brand.getId(), member.getLoginId(), "Password123!");

            // act - 두 번째 좋아요 (취소)
            ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> response = toggleBrandLike(
                brand.getId(), member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().liked()).isFalse(),
                () -> assertThat(response.getBody().data().likeCount()).isEqualTo(0L)
            );
        }

        @Test
        @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenAuthenticationFails() {
            // act
            ResponseEntity<ApiResponse<Object>> response = toggleBrandLikeWithError(
                brand.getId(), member.getLoginId(), "WrongPassword!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("존재하지 않는 브랜드에 좋아요 시 404 Not Found를 반환한다")
        void returnsNotFound_whenBrandNotExists() {
            // act
            ResponseEntity<ApiResponse<Object>> response = toggleBrandLikeWithError(
                999L, member.getLoginId(), "Password123!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        private ResponseEntity<ApiResponse<LikeV1Dto.LikeResponse>> toggleBrandLike(
            Long brandId, String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            ParameterizedTypeReference<ApiResponse<LikeV1Dto.LikeResponse>> responseType =
                new ParameterizedTypeReference<>() {};

            return testRestTemplate.exchange(
                "/api/v1/brands/" + brandId + "/like",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType
            );
        }

        private ResponseEntity<ApiResponse<Object>> toggleBrandLikeWithError(
            Long brandId, String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};

            return testRestTemplate.exchange(
                "/api/v1/brands/" + brandId + "/like",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType
            );
        }
    }

    private Member saveMember(String loginId, String rawPassword) {
        Member member = new Member(loginId, rawPassword, "Test User",
            LocalDate.of(1990, 1, 1), "test@example.com");
        member.encryptPassword(passwordEncoder.encode(rawPassword));
        return memberRepository.save(member);
    }

    private Brand saveBrand(String name) {
        Brand brand = new Brand(name, "Description", "https://example.com/logo.png");
        return brandRepository.save(brand);
    }

    private Product saveProduct(String name, Long brandId) {
        Product product = new Product(name, brandId, 1L, 10000L);
        return productRepository.save(product);
    }
}
