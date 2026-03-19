package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest(
    classes = {
        PgClientResilienceTest.TestConfig.class,
        AopAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class
    },
    properties = {
        "resilience4j.circuitbreaker.instances.pgPayment.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.pgPayment.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.pgPayment.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.pgPayment.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.pgPayment.permitted-number-of-calls-in-half-open-state=3",
        "resilience4j.circuitbreaker.instances.pgPayment.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.pgPayment.record-exceptions[0]=org.springframework.web.client.ResourceAccessException",
        "resilience4j.circuitbreaker.instances.pgPayment.record-exceptions[1]=org.springframework.web.client.HttpServerErrorException",
        "resilience4j.circuitbreaker.instances.pgPayment.ignore-exceptions[0]=org.springframework.web.client.HttpClientErrorException",
        "resilience4j.circuitbreaker.instances.pgQuery.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.pgQuery.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.pgQuery.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.pgQuery.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.pgQuery.permitted-number-of-calls-in-half-open-state=3",
        "resilience4j.circuitbreaker.instances.pgQuery.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.pgQuery.record-exceptions[0]=org.springframework.web.client.ResourceAccessException",
        "resilience4j.circuitbreaker.instances.pgQuery.record-exceptions[1]=org.springframework.web.client.HttpServerErrorException",
        "resilience4j.circuitbreaker.instances.pgQuery.ignore-exceptions[0]=org.springframework.web.client.HttpClientErrorException"
    }
)
class PgClientResilienceTest {

    @TestConfiguration
    static class TestConfig {
        @Bean("pgRestTemplate")
        public RestTemplate pgRestTemplate() {
            return mock(RestTemplate.class);
        }

        @Bean
        public PgClient pgClient(RestTemplate pgRestTemplate, CircuitBreakerRegistry circuitBreakerRegistry) {
            PgClient client = new PgClient(pgRestTemplate, circuitBreakerRegistry);
            org.springframework.test.util.ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:8082");
            org.springframework.test.util.ReflectionTestUtils.setField(client, "callbackUrl", "http://localhost:8080/callback");
            return client;
        }
    }

    @Autowired
    private PgClient pgClient;

    @Autowired
    private RestTemplate pgRestTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.reset(pgRestTemplate);
        circuitBreakerRegistry.getAllCircuitBreakers()
            .forEach(CircuitBreaker::reset);
    }

    @DisplayName("CircuitBreaker")
    @Nested
    class CircuitBreakerTest {

        @Test
        @DisplayName("CLOSED 상태에서 정상 호출이 통과한다")
        void circuitBreaker_closedState_callPassesThrough() {
            // arrange
            PgPaymentResponse pgResponse = new PgPaymentResponse("TXN-001", "ACCEPTED", "결제 요청 접수");
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willReturn(ResponseEntity.ok(pgResponse));

            // act
            PaymentGatewayResponse result = pgClient.requestPayment(
                "ORD-001", "VISA", "4111111111111111", 10000L, 1L);

            // assert
            assertThat(result.transactionId()).isEqualTo("TXN-001");
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pgPayment");
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("실패율 초과 시 OPEN 상태로 전환된다")
        void circuitBreaker_opensAfterFailureThreshold() {
            // arrange — minCalls=2, failureRate=50% → 2회 실패면 100% → OPEN
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pgPayment");

            // act
            for (int i = 0; i < 2; i++) {
                try {
                    pgClient.requestPayment("ORD-001", "VISA", "4111111111111111", 10000L, 1L);
                } catch (PaymentGatewayException ignored) {
                }
            }

            // assert
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        }

        @Test
        @DisplayName("OPEN 상태에서 호출이 즉시 차단되고 PaymentGatewayException을 던진다")
        void circuitBreaker_rejectsCallWhenOpen() {
            // arrange
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pgPayment");
            cb.transitionToOpenState();

            // act & assert
            assertThatThrownBy(() -> pgClient.requestPayment(
                "ORD-001", "VISA", "4111111111111111", 10000L, 1L
            ))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isFalse());
        }

        @Test
        @DisplayName("HALF_OPEN 상태에서 성공하면 CLOSED로 복귀한다")
        void circuitBreaker_halfOpenAllowsLimitedCalls() {
            // arrange
            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pgPayment");
            cb.transitionToOpenState();
            cb.transitionToHalfOpenState();

            PgPaymentResponse pgResponse = new PgPaymentResponse("TXN-001", "ACCEPTED", "결제 요청 접수");
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willReturn(ResponseEntity.ok(pgResponse));

            // act — HALF_OPEN에서 permittedCalls(3)만큼 성공
            for (int i = 0; i < 3; i++) {
                pgClient.requestPayment("ORD-001", "VISA", "4111111111111111", 10000L, 1L);
            }

            // assert
            assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }

        @Test
        @DisplayName("HttpClientErrorException은 timeout=false, ResourceAccessException은 timeout=true로 변환된다")
        void circuitBreaker_fallbackPreservesOriginalExceptionType() {
            // arrange — HttpClientErrorException (4xx)
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

            // act & assert
            assertThatThrownBy(() -> pgClient.requestPayment(
                "ORD-001", "VISA", "4111111111111111", 10000L, 1L
            ))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isFalse());

            // arrange — ResourceAccessException (timeout)
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            // act & assert
            assertThatThrownBy(() -> pgClient.requestPayment(
                "ORD-001", "VISA", "4111111111111111", 10000L, 1L
            ))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isTrue());
        }

        @Test
        @DisplayName("pgPayment와 pgQuery는 독립적인 CB 인스턴스이다")
        void circuitBreaker_separateInstances() {
            // arrange — pgPayment OPEN
            CircuitBreaker pgPaymentCb = circuitBreakerRegistry.circuitBreaker("pgPayment");
            pgPaymentCb.transitionToOpenState();

            PgPaymentStatusResponse statusResponse = new PgPaymentStatusResponse(
                "TXN-001", "ORD-001", "SUCCESS", "결제 완료");
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class)
            )).willReturn(ResponseEntity.ok(statusResponse));

            // act — pgQuery는 정상 동작
            PaymentGatewayResponse result = pgClient.getPaymentStatus("TXN-001", 1L);

            // assert
            CircuitBreaker pgQueryCb = circuitBreakerRegistry.circuitBreaker("pgQuery");
            assertThat(result.status()).isEqualTo("SUCCESS");
            assertThat(pgPaymentCb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
            assertThat(pgQueryCb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        }
    }
}
