package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
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

import io.github.resilience4j.springboot3.bulkhead.autoconfigure.BulkheadAutoConfiguration;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    classes = {
        PgClientResilienceTest.TestConfig.class,
        AopAutoConfiguration.class,
        CircuitBreakerAutoConfiguration.class,
        RetryAutoConfiguration.class,
        BulkheadAutoConfiguration.class
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
        "resilience4j.circuitbreaker.instances.pgPayment.ignore-exceptions[1]=io.github.resilience4j.bulkhead.BulkheadFullException",
        "resilience4j.circuitbreaker.instances.pgQuery.sliding-window-type=COUNT_BASED",
        "resilience4j.circuitbreaker.instances.pgQuery.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.pgQuery.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.pgQuery.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.pgQuery.permitted-number-of-calls-in-half-open-state=3",
        "resilience4j.circuitbreaker.instances.pgQuery.minimum-number-of-calls=2",
        "resilience4j.circuitbreaker.instances.pgQuery.record-exceptions[0]=org.springframework.web.client.ResourceAccessException",
        "resilience4j.circuitbreaker.instances.pgQuery.record-exceptions[1]=org.springframework.web.client.HttpServerErrorException",
        "resilience4j.circuitbreaker.instances.pgQuery.ignore-exceptions[0]=org.springframework.web.client.HttpClientErrorException",
        "resilience4j.circuitbreaker.instances.pgQuery.ignore-exceptions[1]=io.github.resilience4j.bulkhead.BulkheadFullException",
        "resilience4j.circuitbreaker.circuit-breaker-aspect-order=1",
        "resilience4j.retry.retry-aspect-order=2",
        "resilience4j.retry.instances.pgQuery.max-attempts=2",
        "resilience4j.retry.instances.pgQuery.wait-duration=100ms",
        "resilience4j.retry.instances.pgQuery.retry-exceptions[0]=org.springframework.web.client.ResourceAccessException",
        "resilience4j.retry.instances.pgQuery.ignore-exceptions[0]=org.springframework.web.client.HttpClientErrorException",
        "resilience4j.bulkhead.instances.pgPayment.max-concurrent-calls=2",
        "resilience4j.bulkhead.instances.pgPayment.max-wait-duration=0ms",
        "resilience4j.bulkhead.instances.pgQuery.max-concurrent-calls=2",
        "resilience4j.bulkhead.instances.pgQuery.max-wait-duration=0ms"
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

    @Autowired
    private BulkheadRegistry bulkheadRegistry;

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

    @DisplayName("Retry")
    @Nested
    class RetryTest {

        @Test
        @DisplayName("일시적 실패 후 재시도 성공 시 정상 응답을 반환한다")
        void retry_succeedsAfterTransientFailure() {
            // arrange — 1차 실패, 2차 성공
            PgPaymentStatusResponse statusResponse = new PgPaymentStatusResponse(
                "TXN-001", "ORD-001", "SUCCESS", "결제 완료");
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class)
            ))
                .willThrow(new ResourceAccessException("Read timed out"))
                .willReturn(ResponseEntity.ok(statusResponse));

            // act
            PaymentGatewayResponse result = pgClient.getPaymentStatus("TXN-001", 1L);

            // assert
            assertThat(result.status()).isEqualTo("SUCCESS");
            verify(pgRestTemplate, times(2)).exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class));
        }

        @Test
        @DisplayName("HttpClientErrorException은 재시도하지 않는다")
        void retry_doesNotRetryOnHttpClientErrorException() {
            // arrange
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class)
            )).willThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "Not Found"));

            // act & assert
            assertThatThrownBy(() -> pgClient.getPaymentStatus("TXN-001", 1L))
                .isInstanceOf(PaymentGatewayException.class);

            verify(pgRestTemplate, times(1)).exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class));
        }

        @Test
        @DisplayName("최대 재시도 횟수 소진 후 PaymentGatewayException(timeout=true)을 던진다")
        void retry_exhaustsMaxAttemptsAndFallbackThrows() {
            // arrange — maxAttempts=2 → 2회 모두 실패
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            // act & assert
            assertThatThrownBy(() -> pgClient.getPaymentStatus("TXN-001", 1L))
                .isInstanceOf(PaymentGatewayException.class)
                .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isTrue());

            verify(pgRestTemplate, times(2)).exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class));
        }

        @Test
        @DisplayName("requestPayment에는 Retry가 적용되지 않는다")
        void retry_notAppliedToRequestPayment() {
            // arrange
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            // act & assert
            assertThatThrownBy(() -> pgClient.requestPayment(
                "ORD-001", "VISA", "4111111111111111", 10000L, 1L
            ))
                .isInstanceOf(PaymentGatewayException.class);

            // RestTemplate 1회만 호출 (Retry 없음)
            verify(pgRestTemplate, times(1)).exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class));
        }

        @Test
        @DisplayName("재시도 모두 실패해도 CB 실패 카운트는 1회만 증가한다")
        void retry_thenCircuitBreakerCountsFinalResult() {
            // arrange — maxAttempts=2 → 2회 모두 실패
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class)
            )).willThrow(new ResourceAccessException("Read timed out"));

            CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("pgQuery");

            // act
            try {
                pgClient.getPaymentStatus("TXN-001", 1L);
            } catch (PaymentGatewayException ignored) {
            }

            // assert — CB 실패 카운트 1회 (Retry 내부 2회 호출이지만 CB에는 1회로 기록)
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            assertThat(metrics.getNumberOfFailedCalls()).isEqualTo(1);
        }
    }

    @DisplayName("Bulkhead")
    @Nested
    class BulkheadTest {

        @Test
        @DisplayName("동시 호출이 maxConcurrentCalls 이하이면 정상 통과한다")
        void bulkhead_allowsCallsWithinLimit() {
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
        }

        @Test
        @DisplayName("동시 호출이 maxConcurrentCalls 초과 시 PaymentGatewayException을 던진다")
        void bulkhead_rejectsCallsExceedingLimit() throws InterruptedException {
            // arrange — maxConcurrentCalls=2, 3개 동시 호출
            CountDownLatch enteredLatch = new CountDownLatch(2);
            CountDownLatch blockLatch = new CountDownLatch(1);

            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willAnswer(invocation -> {
                enteredLatch.countDown();
                blockLatch.await(5, TimeUnit.SECONDS);
                return ResponseEntity.ok(new PgPaymentResponse("TXN-001", "ACCEPTED", "결제 요청 접수"));
            });

            ExecutorService executor = Executors.newFixedThreadPool(3);
            List<Future<?>> futures = new ArrayList<>();

            try {
                // act — 2개 슬롯을 점유하는 호출
                for (int i = 0; i < 2; i++) {
                    futures.add(executor.submit(() ->
                        pgClient.requestPayment("ORD-001", "VISA", "4111111111111111", 10000L, 1L)
                    ));
                }

                // 2개 호출이 RestTemplate에 진입할 때까지 대기
                assertThat(enteredLatch.await(5, TimeUnit.SECONDS)).isTrue();

                // 3번째 호출은 Bulkhead에 의해 거절되어야 함
                assertThatThrownBy(() ->
                    pgClient.requestPayment("ORD-002", "VISA", "4111111111111111", 10000L, 1L)
                )
                    .isInstanceOf(PaymentGatewayException.class)
                    .satisfies(ex -> assertThat(((PaymentGatewayException) ex).isTimeout()).isFalse());
            } finally {
                blockLatch.countDown();
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        }

        @Test
        @DisplayName("pgPayment와 pgQuery Bulkhead는 독립적으로 동작한다")
        void bulkhead_separateInstances() throws InterruptedException {
            // arrange — pgPayment Bulkhead를 가득 채움
            CountDownLatch enteredLatch = new CountDownLatch(2);
            CountDownLatch blockLatch = new CountDownLatch(1);

            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(PgPaymentResponse.class)
            )).willAnswer(invocation -> {
                enteredLatch.countDown();
                blockLatch.await(5, TimeUnit.SECONDS);
                return ResponseEntity.ok(new PgPaymentResponse("TXN-001", "ACCEPTED", "결제 요청 접수"));
            });

            PgPaymentStatusResponse statusResponse = new PgPaymentStatusResponse(
                "TXN-001", "ORD-001", "SUCCESS", "결제 완료");
            given(pgRestTemplate.exchange(
                anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(PgPaymentStatusResponse.class)
            )).willReturn(ResponseEntity.ok(statusResponse));

            ExecutorService executor = Executors.newFixedThreadPool(2);

            try {
                // pgPayment Bulkhead 2슬롯 점유
                for (int i = 0; i < 2; i++) {
                    executor.submit(() ->
                        pgClient.requestPayment("ORD-001", "VISA", "4111111111111111", 10000L, 1L)
                    );
                }
                assertThat(enteredLatch.await(5, TimeUnit.SECONDS)).isTrue();

                // act — pgQuery는 독립 Bulkhead이므로 정상 동작
                PaymentGatewayResponse result = pgClient.getPaymentStatus("TXN-001", 1L);

                // assert
                assertThat(result.status()).isEqualTo("SUCCESS");
            } finally {
                blockLatch.countDown();
                executor.shutdown();
                executor.awaitTermination(5, TimeUnit.SECONDS);
            }
        }
    }
}
