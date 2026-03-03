package com.loopers.interfaces.api.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Coupon Admin V1 API E2E 테스트")
class CouponAdminV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/admin/coupons";
    private static final String VALID_ADMIN_LDAP = "loopers.admin";
    private static final String INVALID_ADMIN_LDAP = "invalid.ldap";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

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

    private Coupon createTestCoupon(String name, CouponType type, Long discountValue) {
        return new Coupon(
            name,
            "테스트 쿠폰 설명",
            type,
            discountValue,
            10000L,
            type == CouponType.RATE ? 5000L : null,
            1000,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(30)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/admin/coupons")
    class GetCoupons {

        @Test
        @DisplayName("Admin이 쿠폰 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            couponRepository.save(createTestCoupon("신규 가입 쿠폰", CouponType.FIXED, 5000L));
            couponRepository.save(createTestCoupon("VIP 할인 쿠폰", CouponType.RATE, 10L));

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
        @DisplayName("페이징이 정상적으로 동작한다")
        void returnsPaginatedCoupons() {
            // Arrange
            for (int i = 0; i < 15; i++) {
                couponRepository.save(createTestCoupon("쿠폰" + i, CouponType.FIXED, 1000L + i));
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
    @DisplayName("GET /api/v1/admin/coupons/{couponId}")
    class GetCoupon {

        @Test
        @DisplayName("Admin이 쿠폰 상세를 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("신규 가입 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("신규 가입 쿠폰"),
                () -> assertThat(response.getBody().data().get("discountValue")).isEqualTo(5000)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 조회하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminRequests() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("신규 가입 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 조회하면 404 Not Found를 반환한다")
        void returnsNotFound_whenCouponNotExists() {
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
    }

    @Nested
    @DisplayName("POST /api/v1/admin/coupons")
    class CreateCoupon {

        @Test
        @DisplayName("Admin이 정액 할인 쿠폰을 생성하면 201 Created를 반환한다")
        void returnsCreated_whenAdminCreatesFixedAmountCoupon() {
            // Arrange
            String requestBody = """
                {
                    "name": "신규 가입 환영 쿠폰",
                    "description": "신규 가입 회원을 위한 5,000원 할인 쿠폰입니다.",
                    "couponType": "FIXED",
                    "discountValue": 5000,
                    "minOrderAmount": 30000,
                    "totalQuantity": 1000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

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
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("신규 가입 환영 쿠폰"),
                () -> assertThat(response.getBody().data().get("couponType")).isEqualTo("FIXED"),
                () -> assertThat(response.getBody().data().get("discountValue")).isEqualTo(5000)
            );
        }

        @Test
        @DisplayName("Admin이 정률 할인 쿠폰을 생성하면 201 Created를 반환한다")
        void returnsCreated_whenAdminCreatesPercentageCoupon() {
            // Arrange
            String requestBody = """
                {
                    "name": "VIP 10% 할인 쿠폰",
                    "description": "VIP 회원 전용 10% 할인 쿠폰입니다.",
                    "couponType": "RATE",
                    "discountValue": 10,
                    "minOrderAmount": 50000,
                    "maxDiscountAmount": 10000,
                    "totalQuantity": 500,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-06-30T23:59:59"
                }
                """;

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
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("VIP 10% 할인 쿠폰"),
                () -> assertThat(response.getBody().data().get("couponType")).isEqualTo("RATE"),
                () -> assertThat(response.getBody().data().get("maxDiscountAmount")).isEqualTo(10000)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 생성하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminCreates() {
            // Arrange
            String requestBody = """
                {
                    "name": "신규 가입 쿠폰",
                    "couponType": "FIXED",
                    "discountValue": 5000,
                    "totalQuantity": 1000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

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
                    "couponType": "FIXED",
                    "discountValue": 5000,
                    "totalQuantity": 1000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

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
        @DisplayName("정률 할인값이 100을 초과하면 400 Bad Request를 반환한다")
        void returnsBadRequest_whenPercentageExceeds100() {
            // Arrange
            String requestBody = """
                {
                    "name": "잘못된 쿠폰",
                    "couponType": "RATE",
                    "discountValue": 150,
                    "totalQuantity": 1000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

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
        @DisplayName("validFrom이 validUntil 이후면 400 Bad Request를 반환한다")
        void returnsBadRequest_whenValidFromAfterValidUntil() {
            // Arrange
            String requestBody = """
                {
                    "name": "잘못된 기간 쿠폰",
                    "couponType": "FIXED",
                    "discountValue": 5000,
                    "totalQuantity": 1000,
                    "validFrom": "2025-12-31T23:59:59",
                    "validUntil": "2025-01-01T00:00:00"
                }
                """;

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
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/coupons/{couponId}")
    class UpdateCoupon {

        @Test
        @DisplayName("Admin이 쿠폰을 수정하면 200 OK를 반환한다")
        void returnsOk_whenAdminUpdates() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("기존 쿠폰", CouponType.FIXED, 5000L));
            String requestBody = """
                {
                    "name": "수정된 쿠폰",
                    "description": "수정된 설명",
                    "couponType": "FIXED",
                    "discountValue": 7000,
                    "minOrderAmount": 20000,
                    "totalQuantity": 2000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

            HttpEntity<String> request = new HttpEntity<>(requestBody, createAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("name")).isEqualTo("수정된 쿠폰"),
                () -> assertThat(response.getBody().data().get("discountValue")).isEqualTo(7000)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 수정하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminUpdates() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("기존 쿠폰", CouponType.FIXED, 5000L));
            String requestBody = """
                {
                    "name": "수정된 쿠폰",
                    "couponType": "FIXED",
                    "discountValue": 7000,
                    "totalQuantity": 2000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

            HttpEntity<String> request = new HttpEntity<>(requestBody, createInvalidAdminHeaders());

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.PUT,
                request,
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 수정하면 404 Not Found를 반환한다")
        void returnsNotFound_whenCouponNotExists() {
            // Arrange
            String requestBody = """
                {
                    "name": "수정된 쿠폰",
                    "couponType": "FIXED",
                    "discountValue": 7000,
                    "totalQuantity": 2000,
                    "validFrom": "2025-01-01T00:00:00",
                    "validUntil": "2025-12-31T23:59:59"
                }
                """;

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
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/coupons/{couponId}")
    class DeleteCoupon {

        @Test
        @DisplayName("Admin이 쿠폰을 삭제하면 200 OK를 반환한다")
        void returnsOk_whenAdminDeletes() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("삭제할 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("삭제 후 쿠폰 조회 시 404 Not Found를 반환한다")
        void returnsNotFound_afterDeletion() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("삭제할 쿠폰", CouponType.FIXED, 5000L));

            // Act - 삭제
            testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createAdminHeaders()),
                new ParameterizedTypeReference<ApiResponse<Object>>() {}
            );

            // Assert - 조회
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> getResponse = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 삭제하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminDeletes() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("삭제할 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId(),
                HttpMethod.DELETE,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 삭제하면 404 Not Found를 반환한다")
        void returnsNotFound_whenCouponNotExists() {
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

    @Nested
    @DisplayName("GET /api/v1/admin/coupons/{couponId}/issues")
    class GetCouponIssues {

        @Test
        @DisplayName("Admin이 쿠폰 발급 내역을 조회하면 200 OK를 반환한다")
        void returnsOk_whenAdminRequests() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));
            memberCouponRepository.save(new MemberCoupon(1L, coupon.getId(), "AAAA-1111-BBBB", coupon.getValidUntil()));
            memberCouponRepository.save(new MemberCoupon(2L, coupon.getId(), "CCCC-2222-DDDD", coupon.getValidUntil()));
            memberCouponRepository.save(new MemberCoupon(3L, coupon.getId(), "EEEE-3333-FFFF", coupon.getValidUntil()));

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issues?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(3)
            );
        }

        @Test
        @DisplayName("Admin이 아닌 사용자가 조회하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNonAdminRequests() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issues?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰의 발급 내역을 조회하면 404 Not Found를 반환한다")
        void returnsNotFound_whenCouponNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999/issues?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("페이징이 정상적으로 동작한다")
        void returnsPaginatedIssues() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));
            for (int i = 0; i < 25; i++) {
                memberCouponRepository.save(new MemberCoupon(
                    (long) (i + 1),
                    coupon.getId(),
                    String.format("TEST-%04d-CODE", i),
                    coupon.getValidUntil()
                ));
            }

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issues?page=0&size=10",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat((List<?>) response.getBody().data().get("content")).hasSize(10),
                () -> assertThat(response.getBody().data().get("totalElements")).isEqualTo(25),
                () -> assertThat(response.getBody().data().get("totalPages")).isEqualTo(3)
            );
        }

        @Test
        @DisplayName("발급 내역에 쿠폰 사용 정보가 포함된다")
        void includesUsageInformation() {
            // Arrange
            Coupon coupon = couponRepository.save(createTestCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));
            MemberCoupon memberCoupon = new MemberCoupon(1L, coupon.getId(), "AAAA-1111-BBBB", coupon.getValidUntil());
            memberCoupon.use(100L);
            memberCouponRepository.save(memberCoupon);

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issues?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(createAdminHeaders()),
                responseType
            );

            // Assert
            List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().data().get("content");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(content).hasSize(1),
                () -> assertThat(content.get(0).get("status")).isEqualTo("USED"),
                () -> assertThat(content.get(0).get("usedOrderId")).isEqualTo(100)
            );
        }
    }
}
