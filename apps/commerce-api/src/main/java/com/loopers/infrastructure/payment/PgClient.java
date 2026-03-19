package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
        io.github.resilience4j.circuitbreaker.CircuitBreaker cb =
            circuitBreakerRegistry.circuitBreaker("pgPayment");
        return cb.getState() != io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN;
    }

    @Override
    @CircuitBreaker(name = "pgPayment", fallbackMethod = "requestPaymentFallback")
    public PaymentGatewayResponse requestPayment(String orderNumber, String cardType, String cardNo,
                                                  Long amount, Long memberId) {
        String url = baseUrl + "/api/v1/payments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-USER-ID", String.valueOf(memberId));

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
    @CircuitBreaker(name = "pgQuery", fallbackMethod = "getPaymentStatusFallback")
    public PaymentGatewayResponse getPaymentStatus(String transactionId, Long memberId) {
        String url = baseUrl + "/api/v1/payments/" + transactionId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));

        ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), PgPaymentStatusResponse.class
        );
        PgPaymentStatusResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
        return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
    }

    @Override
    @CircuitBreaker(name = "pgQuery", fallbackMethod = "getPaymentByOrderIdFallback")
    public PaymentGatewayResponse getPaymentByOrderId(String orderNumber, Long memberId) {
        String url = baseUrl + "/api/v1/payments?orderId=" + orderNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));

        ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), PgPaymentStatusResponse.class
        );
        PgPaymentStatusResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
        return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
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
