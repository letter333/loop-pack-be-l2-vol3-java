package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State;

@Component
public class PgClient implements PaymentGateway {

    private final RestTemplate restTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PgClient(@Qualifier("pgRestTemplate") RestTemplate restTemplate,
                    CircuitBreakerRegistry circuitBreakerRegistry) {
        this.restTemplate = restTemplate;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Value("${pg.base-url}")
    private String baseUrl;

    @Value("${pg.callback-url}")
    private String callbackUrl;

    @Override
    public boolean isAvailable() {
        var cb = circuitBreakerRegistry.circuitBreaker("pgPayment");
        return cb.getState() != State.OPEN;
    }

    @Override
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "requestPaymentFallback")
    @Bulkhead(name = "pgPayment", fallbackMethod = "requestPaymentBulkheadFallback")
    public PaymentGatewayResponse requestPayment(String orderNumber, String cardType, String cardNo,
                                                  Long amount, Long memberId) {
        String url = baseUrl + "/api/v1/payments";

        HttpHeaders headers = createHeaders(memberId);
        headers.setContentType(MediaType.APPLICATION_JSON);

        PgPaymentRequest request = new PgPaymentRequest(
            orderNumber, cardType, cardNo, String.valueOf(amount), callbackUrl
        );

        ResponseEntity<PgPaymentResponse> response = restTemplate.exchange(
            url, HttpMethod.POST, new HttpEntity<>(request, headers), PgPaymentResponse.class
        );
        PgPaymentResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
        return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
    }

    @Override
    @Retry(name = "pgQuery")
    @CircuitBreaker(name = "pgQuery", fallbackMethod = "getPaymentStatusFallback")
    @Bulkhead(name = "pgQuery", fallbackMethod = "getPaymentStatusBulkheadFallback")
    public PaymentGatewayResponse getPaymentStatus(String transactionId, Long memberId) {
        String url = baseUrl + "/api/v1/payments/" + transactionId;

        ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(createHeaders(memberId)), PgPaymentStatusResponse.class
        );
        PgPaymentStatusResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
        return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
    }

    @Override
    @Retry(name = "pgQuery")
    @CircuitBreaker(name = "pgQuery", fallbackMethod = "getPaymentByOrderIdFallback")
    @Bulkhead(name = "pgQuery", fallbackMethod = "getPaymentByOrderIdBulkheadFallback")
    public PaymentGatewayResponse getPaymentByOrderId(String orderNumber, Long memberId) {
        String url = baseUrl + "/api/v1/payments?orderId=" + orderNumber;

        ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(createHeaders(memberId)), PgPaymentStatusResponse.class
        );
        PgPaymentStatusResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
        return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
    }

    private HttpHeaders createHeaders(Long memberId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));
        return headers;
    }

    private PaymentGatewayResponse requestPaymentFallback(String orderNumber, String cardType, String cardNo,
                                                           Long amount, Long memberId, Throwable t) {
        throw toPaymentGatewayException("PG 결제 요청", t);
    }

    private PaymentGatewayResponse getPaymentStatusFallback(String transactionId, Long memberId, Throwable t) {
        throw toPaymentGatewayException("PG 결제 상태 조회", t);
    }

    private PaymentGatewayResponse getPaymentByOrderIdFallback(String orderNumber, Long memberId, Throwable t) {
        throw toPaymentGatewayException("PG 결제 상태 조회", t);
    }

    private PaymentGatewayResponse requestPaymentBulkheadFallback(String orderNumber, String cardType, String cardNo,
                                                                    Long amount, Long memberId, BulkheadFullException e) {
        throw new PaymentGatewayException("PG 결제 요청 과부하: 동시 요청 한도 초과", false, e);
    }

    private PaymentGatewayResponse getPaymentStatusBulkheadFallback(String transactionId, Long memberId,
                                                                     BulkheadFullException e) {
        throw new PaymentGatewayException("PG 결제 상태 조회 과부하: 동시 요청 한도 초과", false, e);
    }

    private PaymentGatewayResponse getPaymentByOrderIdBulkheadFallback(String orderNumber, Long memberId,
                                                                        BulkheadFullException e) {
        throw new PaymentGatewayException("PG 결제 상태 조회 과부하: 동시 요청 한도 초과", false, e);
    }

    private PaymentGatewayException toPaymentGatewayException(String context, Throwable t) {
        if (t instanceof ResourceAccessException) {
            return new PaymentGatewayException(context + " 타임아웃: " + t.getMessage(), true, t);
        }
        if (t instanceof HttpClientErrorException) {
            return new PaymentGatewayException(
                context + " 실패: " + ((HttpClientErrorException) t).getResponseBodyAsString(), false, t);
        }
        return new PaymentGatewayException(context + " 실패: " + t.getMessage(), false, t);
    }
}
