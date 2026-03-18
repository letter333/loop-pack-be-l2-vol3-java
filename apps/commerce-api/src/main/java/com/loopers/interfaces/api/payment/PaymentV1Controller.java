package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentV1Controller implements PaymentV1ApiSpec {

    private final PaymentFacade paymentFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Override
    public ApiResponse<PaymentV1Dto.PaymentResponse> requestPayment(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @Valid @RequestBody PaymentV1Dto.CreatePaymentRequest request) {
        PaymentInfo info = paymentFacade.requestPayment(loginId, password, request.toCommand());
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(info));
    }

    @PostMapping("/callback")
    @Override
    public ApiResponse<PaymentV1Dto.PaymentResponse> handleCallback(
        @Valid @RequestBody PaymentV1Dto.CallbackRequest request) {
        PaymentInfo info = paymentFacade.handleCallback(request.toCommand());
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(info));
    }

    @PostMapping("/{paymentId}/recover")
    @Override
    public ApiResponse<PaymentV1Dto.PaymentResponse> recoverPayment(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long paymentId) {
        PaymentInfo info = paymentFacade.recoverPayment(loginId, password, paymentId);
        return ApiResponse.success(PaymentV1Dto.PaymentResponse.from(info));
    }
}
