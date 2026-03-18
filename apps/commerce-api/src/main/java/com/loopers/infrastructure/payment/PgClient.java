package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
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

    public PgClient(@Qualifier("pgRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${pg.base-url}")
    private String baseUrl;

    @Value("${pg.callback-url}")
    private String callbackUrl;

    @Override
    public PaymentGatewayResponse requestPayment(String orderNumber, String cardType, String cardNo,
                                                  Long amount, Long memberId) {
        String url = baseUrl + "/api/v1/payments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-USER-ID", String.valueOf(memberId));

        PgPaymentRequest request = new PgPaymentRequest(
            orderNumber, cardType, cardNo, String.valueOf(amount), callbackUrl
        );

        try {
            ResponseEntity<PgPaymentResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request, headers), PgPaymentResponse.class
            );
            PgPaymentResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
            return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
        } catch (HttpClientErrorException e) {
            throw new PaymentGatewayException(
                "PG 결제 요청 실패: " + e.getResponseBodyAsString(), false, e);
        } catch (ResourceAccessException e) {
            throw new PaymentGatewayException(
                "PG 결제 요청 타임아웃: " + e.getMessage(), true, e);
        }
    }

    @Override
    public PaymentGatewayResponse getPaymentStatus(String transactionId, Long memberId) {
        String url = baseUrl + "/api/v1/payments/" + transactionId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));

        try {
            ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), PgPaymentStatusResponse.class
            );
            PgPaymentStatusResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
            return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
        } catch (HttpClientErrorException e) {
            throw new PaymentGatewayException(
                "PG 결제 상태 조회 실패: " + e.getResponseBodyAsString(), false, e);
        } catch (ResourceAccessException e) {
            throw new PaymentGatewayException(
                "PG 결제 상태 조회 타임아웃: " + e.getMessage(), true, e);
        }
    }

    @Override
    public PaymentGatewayResponse getPaymentByOrderId(String orderNumber, Long memberId) {
        String url = baseUrl + "/api/v1/payments?orderId=" + orderNumber;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));

        try {
            ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), PgPaymentStatusResponse.class
            );
            PgPaymentStatusResponse body = Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
            return new PaymentGatewayResponse(body.transactionId(), body.status(), body.message());
        } catch (HttpClientErrorException e) {
            throw new PaymentGatewayException(
                "PG 결제 상태 조회 실패: " + e.getResponseBodyAsString(), false, e);
        } catch (ResourceAccessException e) {
            throw new PaymentGatewayException(
                "PG 결제 상태 조회 타임아웃: " + e.getMessage(), true, e);
        }
    }
}
