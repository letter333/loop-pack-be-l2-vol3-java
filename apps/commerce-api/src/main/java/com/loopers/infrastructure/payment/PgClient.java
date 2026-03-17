package com.loopers.infrastructure.payment;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Component
public class PgClient {

    private final RestTemplate restTemplate;

    public PgClient(@Qualifier("pgRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Value("${pg.base-url}")
    private String baseUrl;

    @Value("${pg.callback-url}")
    private String callbackUrl;

    public PgPaymentResponse requestPayment(String orderId, String cardType, String cardNo,
                                             Long amount, Long memberId) {
        String url = baseUrl + "/api/v1/payments";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-USER-ID", String.valueOf(memberId));

        PgPaymentRequest request = new PgPaymentRequest(
            orderId, cardType, cardNo, String.valueOf(amount), callbackUrl
        );

        try {
            ResponseEntity<PgPaymentResponse> response = restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(request, headers), PgPaymentResponse.class
            );
            return Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
        } catch (HttpClientErrorException e) {
            throw new PgPaymentFailedException("PG 결제 요청 실패: " + e.getResponseBodyAsString());
        }
    }

    public PgPaymentStatusResponse getPaymentStatus(String transactionId, Long memberId) {
        String url = baseUrl + "/api/v1/payments/" + transactionId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));

        ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), PgPaymentStatusResponse.class
        );
        return Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
    }

    public PgPaymentStatusResponse getPaymentByOrderId(String orderId, Long memberId) {
        String url = baseUrl + "/api/v1/payments?orderId=" + orderId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-USER-ID", String.valueOf(memberId));

        ResponseEntity<PgPaymentStatusResponse> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), PgPaymentStatusResponse.class
        );
        return Objects.requireNonNull(response.getBody(), "PG 응답 본문이 없습니다.");
    }
}
