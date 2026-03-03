package com.loopers.interfaces.api.coupon;

import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponRepository;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponRepository;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Coupon V1 API E2E 테스트")
class CouponV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/coupons";
    private static final String TEST_LOGIN_ID = "testuser";
    private static final String TEST_PASSWORD = "Password123!";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = saveMember(TEST_LOGIN_ID, TEST_PASSWORD);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Member saveMember(String loginId, String password) {
        Member member = new Member(
            loginId,
            passwordEncoder.encode(password),
            "테스트유저",
            LocalDate.of(1990, 1, 1),
            "test@example.com"
        );
        return memberRepository.save(member);
    }

    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", TEST_LOGIN_ID);
        headers.set("X-Loopers-LoginPw", TEST_PASSWORD);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders createInvalidAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", "invalid");
        headers.set("X-Loopers-LoginPw", "invalid");
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Coupon createIssuableCoupon(String name, CouponType type, Long discountValue) {
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

    private Coupon createExpiredCoupon(String name) {
        return new Coupon(
            name,
            "만료된 쿠폰",
            CouponType.FIXED,
            5000L,
            10000L,
            null,
            1000,
            LocalDateTime.now().minusDays(30),
            LocalDateTime.now().minusDays(1)
        );
    }

    @Nested
    @DisplayName("GET /api/v1/coupons")
    class GetIssuableCoupons {

        @Test
        @DisplayName("로그인한 사용자가 발급 가능한 쿠폰 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenAuthenticatedUserRequests() {
            // Arrange
            couponRepository.save(createIssuableCoupon("신규 가입 쿠폰", CouponType.FIXED, 5000L));
            couponRepository.save(createIssuableCoupon("VIP 할인 쿠폰", CouponType.RATE, 10L));

            // Act
            ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(2)
            );
        }

        @Test
        @DisplayName("인증되지 않은 사용자가 조회하면 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenUnauthenticatedUserRequests() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAuthHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("발급 기간이 지난 쿠폰은 목록에 포함되지 않는다")
        void excludesExpiredCoupons() {
            // Arrange
            couponRepository.save(createIssuableCoupon("발급 가능 쿠폰", CouponType.FIXED, 5000L));
            couponRepository.save(createExpiredCoupon("만료된 쿠폰"));

            // Act
            ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(1),
                () -> assertThat(response.getBody().data().get(0).get("name")).isEqualTo("발급 가능 쿠폰")
            );
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰은 isIssued가 true로 표시된다")
        void showsIsIssuedTrue_whenAlreadyIssued() {
            // Arrange
            Coupon coupon = couponRepository.save(createIssuableCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));
            MemberCoupon memberCoupon = new MemberCoupon(
                testMember.getId(),
                coupon.getId(),
                "ABCD-1234-EFGH",
                coupon.getValidUntil()
            );
            memberCouponRepository.save(memberCoupon);

            // Act
            ParameterizedTypeReference<ApiResponse<List<Map<String, Object>>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<List<Map<String, Object>>>> response = testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(1),
                () -> assertThat(response.getBody().data().get(0).get("isIssued")).isEqualTo(true)
            );
        }
    }

    @Nested
    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    class IssueCoupon {

        @Test
        @DisplayName("로그인한 사용자가 쿠폰을 발급받으면 201 Created를 반환한다")
        void returnsCreated_whenSuccessfullyIssued() {
            // Arrange
            Coupon coupon = couponRepository.save(createIssuableCoupon("신규 가입 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().get("couponId")).isEqualTo(coupon.getId().intValue()),
                () -> assertThat(response.getBody().data().get("couponCode")).isNotNull(),
                () -> assertThat(response.getBody().data().get("status")).isEqualTo("AVAILABLE")
            );
        }

        @Test
        @DisplayName("쿠폰 발급 시 랜덤 코드가 XXXX-XXXX-XXXX 형식으로 생성된다")
        void generatesCouponCodeInCorrectFormat() {
            // Arrange
            Coupon coupon = couponRepository.save(createIssuableCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            String couponCode = (String) response.getBody().data().get("couponCode");
            assertThat(couponCode).matches("[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}");
        }

        @Test
        @DisplayName("인증되지 않은 사용자가 발급하면 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenUnauthenticatedUserRequests() {
            // Arrange
            Coupon coupon = couponRepository.save(createIssuableCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(createInvalidAuthHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("존재하지 않는 쿠폰을 발급하면 404 Not Found를 반환한다")
        void returnsNotFound_whenCouponNotExists() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/99999/issue",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("이미 발급받은 쿠폰을 다시 발급하면 409 Conflict를 반환한다")
        void returnsConflict_whenAlreadyIssued() {
            // Arrange
            Coupon coupon = couponRepository.save(createIssuableCoupon("테스트 쿠폰", CouponType.FIXED, 5000L));
            MemberCoupon memberCoupon = new MemberCoupon(
                testMember.getId(),
                coupon.getId(),
                "ABCD-1234-EFGH",
                coupon.getValidUntil()
            );
            memberCouponRepository.save(memberCoupon);

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("발급 기간이 아닌 쿠폰을 발급하면 400 Bad Request를 반환한다")
        void returnsBadRequest_whenOutsideIssuePeriod() {
            // Arrange
            Coupon coupon = couponRepository.save(createExpiredCoupon("만료된 쿠폰"));

            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/" + coupon.getId() + "/issue",
                HttpMethod.POST,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/coupons/my")
    class GetMyCoupons {

        @Test
        @DisplayName("로그인한 사용자가 내 쿠폰 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenAuthenticatedUserRequests() {
            // Arrange
            Coupon coupon1 = couponRepository.save(createIssuableCoupon("쿠폰1", CouponType.FIXED, 5000L));
            Coupon coupon2 = couponRepository.save(createIssuableCoupon("쿠폰2", CouponType.RATE, 10L));
            memberCouponRepository.save(new MemberCoupon(testMember.getId(), coupon1.getId(), "AAAA-1111-BBBB", coupon1.getValidUntil()));
            memberCouponRepository.save(new MemberCoupon(testMember.getId(), coupon2.getId(), "CCCC-2222-DDDD", coupon2.getValidUntil()));

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/my",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().get("totalCount")).isEqualTo(2)
            );
        }

        @Test
        @DisplayName("인증되지 않은 사용자가 조회하면 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenUnauthenticatedUserRequests() {
            // Act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response = testRestTemplate.exchange(
                ENDPOINT + "/my",
                HttpMethod.GET,
                new HttpEntity<>(createInvalidAuthHeaders()),
                responseType
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("status 파라미터로 AVAILABLE 쿠폰만 필터링할 수 있다")
        void filtersAvailableCoupons() {
            // Arrange
            Coupon coupon1 = couponRepository.save(createIssuableCoupon("쿠폰1", CouponType.FIXED, 5000L));
            Coupon coupon2 = couponRepository.save(createIssuableCoupon("쿠폰2", CouponType.FIXED, 3000L));
            MemberCoupon mc1 = memberCouponRepository.save(new MemberCoupon(testMember.getId(), coupon1.getId(), "AAAA-1111-BBBB", coupon1.getValidUntil()));
            MemberCoupon mc2 = memberCouponRepository.save(new MemberCoupon(testMember.getId(), coupon2.getId(), "CCCC-2222-DDDD", coupon2.getValidUntil()));

            // 두 번째 쿠폰 사용 처리
            mc2.use(1L);
            memberCouponRepository.save(mc2);

            // Act
            ParameterizedTypeReference<ApiResponse<Map<String, Object>>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Map<String, Object>>> response = testRestTemplate.exchange(
                ENDPOINT + "/my?status=AVAILABLE",
                HttpMethod.GET,
                new HttpEntity<>(createAuthHeaders()),
                responseType
            );

            // Assert
            List<?> coupons = (List<?>) response.getBody().data().get("coupons");
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(coupons).hasSize(1)
            );
        }
    }
}
