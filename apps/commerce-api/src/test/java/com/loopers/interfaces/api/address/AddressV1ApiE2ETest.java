package com.loopers.interfaces.api.address;

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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AddressV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public AddressV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        MemberRepository memberRepository,
        PasswordEncoder passwordEncoder,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/addresses - 배송지 목록 조회")
    @Nested
    class GetAddresses {

        private Member member;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
        }

        @Test
        @DisplayName("배송지 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenGetAddresses() {
            // arrange
            registerAddress(member.getLoginId(), "Password123!", "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호");
            registerAddress(member.getLoginId(), "Password123!", "김철수", "010-9999-8888", "12345", "부산시 해운대구", "201호");

            // act
            ResponseEntity<ApiResponse<List<AddressV1Dto.AddressResponse>>> response = getAddresses(
                member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(2)
            );
        }

        @Test
        @DisplayName("배송지가 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenNoAddresses() {
            // act
            ResponseEntity<ApiResponse<List<AddressV1Dto.AddressResponse>>> response = getAddresses(
                member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).isEmpty()
            );
        }

        @Test
        @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenAuthenticationFails() {
            // act
            ResponseEntity<ApiResponse<Object>> response = getAddressesWithError(
                member.getLoginId(), "WrongPassword!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        private ResponseEntity<ApiResponse<List<AddressV1Dto.AddressResponse>>> getAddresses(String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> getAddressesWithError(String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("POST /api/v1/addresses - 배송지 등록")
    @Nested
    class RegisterAddress {

        private Member member;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
        }

        @Test
        @DisplayName("배송지를 등록하면 201 Created를 반환한다")
        void returnsCreated_whenRegisterAddress() {
            // act
            ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> response = registerAddressWithResponse(
                member.getLoginId(), "Password123!",
                "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().recipientName()).isEqualTo("홍길동"),
                () -> assertThat(response.getBody().data().isDefault()).isTrue()
            );
        }

        @Test
        @DisplayName("첫 번째 배송지는 자동으로 기본 배송지로 설정된다")
        void setsAsDefault_whenFirstAddress() {
            // act
            ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> response = registerAddressWithResponse(
                member.getLoginId(), "Password123!",
                "홍길동", "010-1234-5678", null, "서울시", null
            );

            // assert
            assertThat(response.getBody().data().isDefault()).isTrue();
        }

        @Test
        @DisplayName("두 번째 이후 배송지는 기본 배송지로 설정되지 않는다")
        void doesNotSetAsDefault_whenNotFirstAddress() {
            // arrange
            registerAddress(member.getLoginId(), "Password123!", "홍길동", "010-1234-5678", null, "서울시", null);

            // act
            ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> response = registerAddressWithResponse(
                member.getLoginId(), "Password123!",
                "김철수", "010-9999-8888", null, "부산시", null
            );

            // assert
            assertThat(response.getBody().data().isDefault()).isFalse();
        }

        @Test
        @DisplayName("배송지가 5개를 초과하면 400 Bad Request를 반환한다")
        void returnsBadRequest_whenExceedsMaxAddresses() {
            // arrange
            for (int i = 1; i <= 5; i++) {
                registerAddress(member.getLoginId(), "Password123!", "홍길동" + i, "010-1234-567" + i, null, "서울시", null);
            }

            // act
            ResponseEntity<ApiResponse<Object>> response = registerAddressWithError(
                member.getLoginId(), "Password123!",
                "김철수", "010-9999-8888", null, "부산시", null
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenAuthenticationFails() {
            // act
            ResponseEntity<ApiResponse<Object>> response = registerAddressWithError(
                member.getLoginId(), "WrongPassword!",
                "홍길동", "010-1234-5678", null, "서울시", null
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        private ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> registerAddressWithResponse(
            String loginId, String password, String recipientName, String phone, String zipCode, String address, String addressDetail
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            headers.setContentType(MediaType.APPLICATION_JSON);

            AddressV1Dto.RegisterRequest request = new AddressV1Dto.RegisterRequest(
                recipientName, phone, zipCode, address, addressDetail
            );

            return testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> registerAddressWithError(
            String loginId, String password, String recipientName, String phone, String zipCode, String address, String addressDetail
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            headers.setContentType(MediaType.APPLICATION_JSON);

            AddressV1Dto.RegisterRequest request = new AddressV1Dto.RegisterRequest(
                recipientName, phone, zipCode, address, addressDetail
            );

            return testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("PUT /api/v1/addresses/{addressId} - 배송지 수정")
    @Nested
    class UpdateAddress {

        private Member member;
        private Long addressId;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            addressId = registerAddressAndGetId(member.getLoginId(), "Password123!", "홍길동", "010-1234-5678", "06234", "서울시", "101호");
        }

        @Test
        @DisplayName("배송지를 수정하면 200 OK를 반환한다")
        void returnsOk_whenUpdateAddress() {
            // act
            ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> response = updateAddress(
                addressId, member.getLoginId(), "Password123!",
                "김철수", "010-9999-8888", "12345", "부산시", "201호"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().recipientName()).isEqualTo("김철수"),
                () -> assertThat(response.getBody().data().phone()).isEqualTo("010-9999-8888")
            );
        }

        @Test
        @DisplayName("존재하지 않는 배송지를 수정하면 404 Not Found를 반환한다")
        void returnsNotFound_whenAddressNotExists() {
            // act
            ResponseEntity<ApiResponse<Object>> response = updateAddressWithError(
                999L, member.getLoginId(), "Password123!",
                "김철수", "010-9999-8888", null, "부산시", null
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 배송지를 수정하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotOwner() {
            // arrange
            Member otherMember = saveMember("user2", "Password123!");

            // act
            ResponseEntity<ApiResponse<Object>> response = updateAddressWithError(
                addressId, otherMember.getLoginId(), "Password123!",
                "김철수", "010-9999-8888", null, "부산시", null
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        private ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> updateAddress(
            Long addressId, String loginId, String password,
            String recipientName, String phone, String zipCode, String address, String addressDetail
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            headers.setContentType(MediaType.APPLICATION_JSON);

            AddressV1Dto.UpdateRequest request = new AddressV1Dto.UpdateRequest(
                recipientName, phone, zipCode, address, addressDetail
            );

            return testRestTemplate.exchange(
                "/api/v1/addresses/" + addressId,
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> updateAddressWithError(
            Long addressId, String loginId, String password,
            String recipientName, String phone, String zipCode, String address, String addressDetail
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            headers.setContentType(MediaType.APPLICATION_JSON);

            AddressV1Dto.UpdateRequest request = new AddressV1Dto.UpdateRequest(
                recipientName, phone, zipCode, address, addressDetail
            );

            return testRestTemplate.exchange(
                "/api/v1/addresses/" + addressId,
                HttpMethod.PUT,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("DELETE /api/v1/addresses/{addressId} - 배송지 삭제")
    @Nested
    class DeleteAddress {

        private Member member;
        private Long addressId;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            addressId = registerAddressAndGetId(member.getLoginId(), "Password123!", "홍길동", "010-1234-5678", "06234", "서울시", "101호");
        }

        @Test
        @DisplayName("배송지를 삭제하면 200 OK를 반환한다")
        void returnsOk_whenDeleteAddress() {
            // act
            ResponseEntity<ApiResponse<Void>> response = deleteAddress(addressId, member.getLoginId(), "Password123!");

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("삭제된 배송지는 목록에서 조회되지 않는다")
        void deletedAddressNotShownInList() {
            // arrange
            deleteAddress(addressId, member.getLoginId(), "Password123!");

            // act
            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            ResponseEntity<ApiResponse<List<AddressV1Dto.AddressResponse>>> response = testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getBody().data()).isEmpty();
        }

        @Test
        @DisplayName("존재하지 않는 배송지를 삭제하면 404 Not Found를 반환한다")
        void returnsNotFound_whenAddressNotExists() {
            // act
            ResponseEntity<ApiResponse<Object>> response = deleteAddressWithError(999L, member.getLoginId(), "Password123!");

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 배송지를 삭제하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotOwner() {
            // arrange
            Member otherMember = saveMember("user2", "Password123!");

            // act
            ResponseEntity<ApiResponse<Object>> response = deleteAddressWithError(addressId, otherMember.getLoginId(), "Password123!");

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("삭제 후 남은 배송지가 1개이면 자동으로 기본 배송지로 설정된다")
        void setsAsDefault_whenOneAddressRemains() {
            // arrange - 두 번째 배송지 등록 (첫 번째는 setUp에서 등록됨, 기본 배송지)
            Long secondAddressId = registerAddressAndGetId(member.getLoginId(), "Password123!", "김철수", "010-9999-8888", null, "부산시", null);

            // 첫 번째 배송지(기본 배송지) 삭제
            deleteAddress(addressId, member.getLoginId(), "Password123!");

            // act - 남은 배송지 목록 조회
            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            ResponseEntity<ApiResponse<List<AddressV1Dto.AddressResponse>>> response = testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            List<AddressV1Dto.AddressResponse> addresses = response.getBody().data();
            assertAll(
                () -> assertThat(addresses).hasSize(1),
                () -> assertThat(addresses.get(0).id()).isEqualTo(secondAddressId),
                () -> assertThat(addresses.get(0).isDefault()).isTrue()
            );
        }

        private ResponseEntity<ApiResponse<Void>> deleteAddress(Long addressId, String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/addresses/" + addressId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> deleteAddressWithError(Long addressId, String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/addresses/" + addressId,
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("PATCH /api/v1/addresses/{addressId}/default - 기본 배송지 설정")
    @Nested
    class SetDefaultAddress {

        private Member member;
        private Long addressId1;
        private Long addressId2;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            addressId1 = registerAddressAndGetId(member.getLoginId(), "Password123!", "홍길동", "010-1234-5678", null, "서울시", null);
            addressId2 = registerAddressAndGetId(member.getLoginId(), "Password123!", "김철수", "010-9999-8888", null, "부산시", null);
        }

        @Test
        @DisplayName("기본 배송지를 설정하면 200 OK를 반환한다")
        void returnsOk_whenSetDefault() {
            // act
            ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> response = setDefaultAddress(
                addressId2, member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().isDefault()).isTrue()
            );
        }

        @Test
        @DisplayName("기본 배송지 설정 시 기존 기본 배송지가 해제된다")
        void unsetsExistingDefault_whenSettingNewDefault() {
            // arrange - addressId1이 첫 번째로 등록되어 기본 배송지임
            setDefaultAddress(addressId2, member.getLoginId(), "Password123!");

            // act
            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            ResponseEntity<ApiResponse<List<AddressV1Dto.AddressResponse>>> response = testRestTemplate.exchange(
                "/api/v1/addresses",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );

            // assert
            List<AddressV1Dto.AddressResponse> addresses = response.getBody().data();
            AddressV1Dto.AddressResponse address1 = addresses.stream()
                .filter(a -> a.id().equals(addressId1)).findFirst().orElseThrow();
            AddressV1Dto.AddressResponse address2 = addresses.stream()
                .filter(a -> a.id().equals(addressId2)).findFirst().orElseThrow();

            assertAll(
                () -> assertThat(address1.isDefault()).isFalse(),
                () -> assertThat(address2.isDefault()).isTrue()
            );
        }

        @Test
        @DisplayName("존재하지 않는 배송지를 기본으로 설정하면 404 Not Found를 반환한다")
        void returnsNotFound_whenAddressNotExists() {
            // act
            ResponseEntity<ApiResponse<Object>> response = setDefaultAddressWithError(999L, member.getLoginId(), "Password123!");

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("다른 회원의 배송지를 기본으로 설정하면 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotOwner() {
            // arrange
            Member otherMember = saveMember("user2", "Password123!");

            // act
            ResponseEntity<ApiResponse<Object>> response = setDefaultAddressWithError(
                addressId1, otherMember.getLoginId(), "Password123!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        private ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> setDefaultAddress(Long addressId, String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/addresses/" + addressId + "/default",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> setDefaultAddressWithError(Long addressId, String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/addresses/" + addressId + "/default",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    private HttpHeaders createAuthHeaders(String loginId, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", password);
        return headers;
    }

    private Member saveMember(String loginId, String rawPassword) {
        Member member = new Member(loginId, rawPassword, "Test User",
            LocalDate.of(1990, 1, 1), loginId + "@example.com");
        member.encryptPassword(passwordEncoder.encode(rawPassword));
        return memberRepository.save(member);
    }

    private void registerAddress(String loginId, String password, String recipientName, String phone, String zipCode, String address, String addressDetail) {
        HttpHeaders headers = createAuthHeaders(loginId, password);
        headers.setContentType(MediaType.APPLICATION_JSON);

        AddressV1Dto.RegisterRequest request = new AddressV1Dto.RegisterRequest(
            recipientName, phone, zipCode, address, addressDetail
        );

        testRestTemplate.exchange(
            "/api/v1/addresses",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<ApiResponse<AddressV1Dto.AddressResponse>>() {}
        );
    }

    private Long registerAddressAndGetId(String loginId, String password, String recipientName, String phone, String zipCode, String address, String addressDetail) {
        HttpHeaders headers = createAuthHeaders(loginId, password);
        headers.setContentType(MediaType.APPLICATION_JSON);

        AddressV1Dto.RegisterRequest request = new AddressV1Dto.RegisterRequest(
            recipientName, phone, zipCode, address, addressDetail
        );

        ResponseEntity<ApiResponse<AddressV1Dto.AddressResponse>> response = testRestTemplate.exchange(
            "/api/v1/addresses",
            HttpMethod.POST,
            new HttpEntity<>(request, headers),
            new ParameterizedTypeReference<>() {}
        );

        return response.getBody().data().id();
    }
}
