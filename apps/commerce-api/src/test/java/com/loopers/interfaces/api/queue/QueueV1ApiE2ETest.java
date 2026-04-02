package com.loopers.interfaces.api.queue;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
import com.loopers.domain.queue.QueueTokenService;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
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
@DisplayName("Queue V1 API E2E 테스트")
class QueueV1ApiE2ETest {

    private static final String ENDPOINT = "/api/v1/queue/enter";
    private static final String RAW_PASSWORD = "Password123!";

    private final TestRestTemplate testRestTemplate;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final QueueTokenService queueTokenService;
    private final DatabaseCleanUp databaseCleanUp;
    private final RedisCleanUp redisCleanUp;

    @Autowired
    public QueueV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        QueueTokenService queueTokenService,
        DatabaseCleanUp databaseCleanUp,
        RedisCleanUp redisCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.queueTokenService = queueTokenService;
        this.databaseCleanUp = databaseCleanUp;
        this.redisCleanUp = redisCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @DisplayName("POST /api/v1/queue/enter")
    @Nested
    class Enter {

        private Member member;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", RAW_PASSWORD);
        }

        @Test
        @DisplayName("정상 진입 시 201 Created와 순번 정보를 반환한다")
        void returnsCreated_whenEnterSuccessfully() {
            // act
            ResponseEntity<ApiResponse<QueueV1Dto.EnterResponse>> response = enterQueue(
                member.getLoginId(), RAW_PASSWORD
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().position()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().totalWaiting()).isEqualTo(1L)
            );
        }

        @Test
        @DisplayName("두 번째 유저 진입 시 순번 2를 반환한다")
        void returnsPosition2_whenSecondUserEnters() {
            // arrange
            Member member2 = saveMember("user2", RAW_PASSWORD);
            enterQueue(member.getLoginId(), RAW_PASSWORD);

            // act
            ResponseEntity<ApiResponse<QueueV1Dto.EnterResponse>> response = enterQueue(
                member2.getLoginId(), RAW_PASSWORD
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().position()).isEqualTo(2L),
                () -> assertThat(response.getBody().data().totalWaiting()).isEqualTo(2L)
            );
        }

        @Test
        @DisplayName("중복 진입 시 409 Conflict를 반환한다")
        void returnsConflict_whenDuplicateEntry() {
            // arrange
            enterQueue(member.getLoginId(), RAW_PASSWORD);

            // act
            ResponseEntity<ApiResponse<Object>> response = enterQueueWithError(
                member.getLoginId(), RAW_PASSWORD
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenAuthenticationFails() {
            // act
            ResponseEntity<ApiResponse<Object>> response = enterQueueWithError(
                member.getLoginId(), "WrongPassword!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        private ResponseEntity<ApiResponse<QueueV1Dto.EnterResponse>> enterQueue(
            String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>> responseType =
                new ParameterizedTypeReference<>() {};

            return testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType
            );
        }

        private ResponseEntity<ApiResponse<Object>> enterQueueWithError(
            String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            ParameterizedTypeReference<ApiResponse<Object>> responseType =
                new ParameterizedTypeReference<>() {};

            return testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                responseType
            );
        }
    }

    @DisplayName("GET /api/v1/queue/position")
    @Nested
    class GetPosition {

        private Member member;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", RAW_PASSWORD);
        }

        @Test
        @DisplayName("대기열 진입 후 조회하면 WAITING 상태와 순번을 반환한다")
        void returnsWaiting_whenInQueue() {
            // arrange
            enterQueue(member.getLoginId(), RAW_PASSWORD);

            // act
            ResponseEntity<ApiResponse<QueueV1Dto.PositionResponse>> response = getPosition(
                member.getLoginId(), RAW_PASSWORD
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().status()).isEqualTo("WAITING"),
                () -> assertThat(response.getBody().data().position()).isEqualTo(1L),
                () -> assertThat(response.getBody().data().token()).isNull()
            );
        }

        @Test
        @DisplayName("토큰이 발급된 유저는 ADMITTED 상태와 토큰을 반환한다")
        void returnsAdmitted_whenTokenIssued() {
            // arrange
            queueTokenService.issueToken("order", member.getId());

            // act
            ResponseEntity<ApiResponse<QueueV1Dto.PositionResponse>> response = getPosition(
                member.getLoginId(), RAW_PASSWORD
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().status()).isEqualTo("ADMITTED"),
                () -> assertThat(response.getBody().data().position()).isEqualTo(0L),
                () -> assertThat(response.getBody().data().token()).isNotNull()
            );
        }

        @Test
        @DisplayName("대기열에도 없고 토큰도 없는 유저는 404를 반환한다")
        void returnsNotFound_whenNotInQueueAndNoToken() {
            // act
            ResponseEntity<ApiResponse<Object>> response = getPositionWithError(
                member.getLoginId(), RAW_PASSWORD
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        private ResponseEntity<ApiResponse<QueueV1Dto.PositionResponse>> getPosition(
            String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            return testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> getPositionWithError(
            String loginId, String password
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            return testRestTemplate.exchange(
                "/api/v1/queue/position",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private void enterQueue(String loginId, String password) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", loginId);
            headers.set("X-Loopers-LoginPw", password);

            testRestTemplate.exchange(
                ENDPOINT,
                HttpMethod.POST,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ApiResponse<QueueV1Dto.EnterResponse>>() {}
            );
        }
    }

    private Member saveMember(String loginId, String rawPassword) {
        Member member = new Member(loginId, rawPassword, "Test User",
            LocalDate.of(1990, 1, 1), loginId + "@example.com");
        member.encryptPassword(passwordEncoder.encode(rawPassword));
        return memberRepository.save(member);
    }
}
