package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PgClientTest {

    @Mock
    private RestTemplate restTemplate;

    private PgClient pgClient;

    private static final String BASE_URL = "http://localhost:8082";
    private static final String CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";
    private static final Long MEMBER_ID = 1L;

    @BeforeEach
    void setUp() {
        pgClient = new PgClient(restTemplate);
        ReflectionTestUtils.setField(pgClient, "baseUrl", BASE_URL);
        ReflectionTestUtils.setField(pgClient, "callbackUrl", CALLBACK_URL);
    }

    @DisplayName("결제 요청")
    @Nested
    class RequestPayment {

        @Test
        @DisplayName("PG에 올바른 URL/헤더/바디로 요청한다")
        @SuppressWarnings("unchecked")
        void sendsCorrectRequest() {
            // arrange
            PgPaymentResponse pgResponse = new PgPaymentResponse("TXN-001", "ACCEPTED", "결제 요청 접수");
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PgPaymentResponse.class)
            )).willReturn(ResponseEntity.ok(pgResponse));

            ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

            // act
            pgClient.requestPayment("ORD20250318-0000001", "VISA", "4111111111111111", 10000L, MEMBER_ID);

            // assert
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments"),
                eq(HttpMethod.POST),
                captor.capture(),
                eq(PgPaymentResponse.class)
            )).willReturn(ResponseEntity.ok(pgResponse));

            pgClient.requestPayment("ORD20250318-0000001", "VISA", "4111111111111111", 10000L, MEMBER_ID);

            HttpEntity<?> captured = captor.getValue();
            assertAll(
                () -> assertThat(captured.getHeaders().get("X-USER-ID")).containsExactly("1"),
                () -> assertThat(captured.getHeaders().getContentType().toString()).contains("application/json"),
                () -> {
                    PgPaymentRequest body = (PgPaymentRequest) captured.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.orderId()).isEqualTo("ORD20250318-0000001");
                    assertThat(body.cardType()).isEqualTo("VISA");
                    assertThat(body.cardNo()).isEqualTo("4111111111111111");
                    assertThat(body.amount()).isEqualTo("10000");
                    assertThat(body.callbackUrl()).isEqualTo(CALLBACK_URL);
                }
            );
        }

        @Test
        @DisplayName("PG가 성공 응답하면 PaymentGatewayResponse를 반환한다")
        void returnsPaymentGatewayResponse_whenPgSucceeds() {
            // arrange
            PgPaymentResponse pgResponse = new PgPaymentResponse("TXN-001", "ACCEPTED", "결제 요청 접수");
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PgPaymentResponse.class)
            )).willReturn(ResponseEntity.ok(pgResponse));

            // act
            PaymentGatewayResponse result = pgClient.requestPayment(
                "ORD20250318-0000001", "VISA", "4111111111111111", 10000L, MEMBER_ID
            );

            // assert
            assertAll(
                () -> assertThat(result.transactionId()).isEqualTo("TXN-001"),
                () -> assertThat(result.status()).isEqualTo("ACCEPTED"),
                () -> assertThat(result.message()).isEqualTo("결제 요청 접수")
            );
        }

        @Test
        @DisplayName("PG가 실패 응답하면 PaymentGatewayException(timeout=false)을 던진다")
        void throwsPaymentGatewayException_whenPgRejects() {
            // arrange
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PgPaymentResponse.class)
            )).willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

            // act & assert
            assertThatThrownBy(() -> pgClient.requestPayment(
                "ORD20250318-0000001", "VISA", "4111111111111111", 10000L, MEMBER_ID
            ))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isFalse());
        }

        @Test
        @DisplayName("PG 호출 타임아웃 시 PaymentGatewayException(timeout=true)을 던진다")
        void throwsPaymentGatewayExceptionWithTimeout_whenTimeout() {
            // arrange
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(PgPaymentResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            // act & assert
            assertThatThrownBy(() -> pgClient.requestPayment(
                "ORD20250318-0000001", "VISA", "4111111111111111", 10000L, MEMBER_ID
            ))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isTrue());
        }
    }

    @DisplayName("결제 상태 조회 (transactionId)")
    @Nested
    class GetPaymentStatus {

        @Test
        @DisplayName("PG에 올바른 URL/헤더로 GET 요청하고 PaymentGatewayResponse를 반환한다")
        void sendsCorrectGetRequestAndReturnsGatewayResponse() {
            // arrange
            PgPaymentStatusResponse statusResponse = new PgPaymentStatusResponse(
                "TXN-001", "ORD20250318-0000001", "SUCCESS", "결제 완료"
            );
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments/TXN-001"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PgPaymentStatusResponse.class)
            )).willReturn(ResponseEntity.ok(statusResponse));

            // act
            PaymentGatewayResponse result = pgClient.getPaymentStatus("TXN-001", MEMBER_ID);

            // assert
            assertAll(
                () -> assertThat(result.transactionId()).isEqualTo("TXN-001"),
                () -> assertThat(result.status()).isEqualTo("SUCCESS"),
                () -> assertThat(result.message()).isEqualTo("결제 완료")
            );
        }

        @Test
        @DisplayName("PG가 4xx 응답하면 PaymentGatewayException(timeout=false)을 던진다")
        void throwsPaymentGatewayException_whenPgRejects() {
            // arrange
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments/TXN-001"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PgPaymentStatusResponse.class)
            )).willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            // act & assert
            assertThatThrownBy(() -> pgClient.getPaymentStatus("TXN-001", MEMBER_ID))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isFalse());
        }

        @Test
        @DisplayName("PG 호출 타임아웃 시 PaymentGatewayException(timeout=true)을 던진다")
        void throwsPaymentGatewayExceptionWithTimeout_whenTimeout() {
            // arrange
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments/TXN-001"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PgPaymentStatusResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            // act & assert
            assertThatThrownBy(() -> pgClient.getPaymentStatus("TXN-001", MEMBER_ID))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isTrue());
        }
    }

    @DisplayName("결제 상태 조회 (orderNumber)")
    @Nested
    class GetPaymentByOrderId {

        @Test
        @DisplayName("PG에 올바른 URL/쿼리파라미터로 GET 요청하고 PaymentGatewayResponse를 반환한다")
        void sendsCorrectGetRequestAndReturnsGatewayResponse() {
            // arrange
            PgPaymentStatusResponse statusResponse = new PgPaymentStatusResponse(
                "TXN-001", "ORD20250318-0000001", "SUCCESS", "결제 완료"
            );
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments?orderId=ORD20250318-0000001"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PgPaymentStatusResponse.class)
            )).willReturn(ResponseEntity.ok(statusResponse));

            // act
            PaymentGatewayResponse result = pgClient.getPaymentByOrderId("ORD20250318-0000001", MEMBER_ID);

            // assert
            assertAll(
                () -> assertThat(result.transactionId()).isEqualTo("TXN-001"),
                () -> assertThat(result.status()).isEqualTo("SUCCESS"),
                () -> assertThat(result.message()).isEqualTo("결제 완료")
            );
        }

        @Test
        @DisplayName("PG가 4xx 응답하면 PaymentGatewayException(timeout=false)을 던진다")
        void throwsPaymentGatewayException_whenPgRejects() {
            // arrange
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments?orderId=ORD20250318-0000001"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PgPaymentStatusResponse.class)
            )).willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            // act & assert
            assertThatThrownBy(() -> pgClient.getPaymentByOrderId("ORD20250318-0000001", MEMBER_ID))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isFalse());
        }

        @Test
        @DisplayName("PG 호출 타임아웃 시 PaymentGatewayException(timeout=true)을 던진다")
        void throwsPaymentGatewayExceptionWithTimeout_whenTimeout() {
            // arrange
            given(restTemplate.exchange(
                eq(BASE_URL + "/api/v1/payments?orderId=ORD20250318-0000001"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(PgPaymentStatusResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            // act & assert
            assertThatThrownBy(() -> pgClient.getPaymentByOrderId("ORD20250318-0000001", MEMBER_ID))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isTrue());
        }
    }
}
