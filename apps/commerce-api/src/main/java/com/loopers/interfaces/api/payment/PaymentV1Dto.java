package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.PaymentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentV1Dto {

    public record CreatePaymentRequest(
        @NotNull(message = "주문 ID는 필수입니다.") Long orderId,
        @NotBlank(message = "카드 종류는 필수입니다.") String cardType,
        @NotBlank(message = "카드 번호는 필수입니다.") String cardNo
    ) {
        public PaymentCommand.Create toCommand() {
            return new PaymentCommand.Create(orderId, cardType, cardNo);
        }
    }

    public record PaymentResponse(
        Long id,
        Long orderId,
        String orderNumber,
        String transactionId,
        String cardType,
        String cardNo,
        Long amount,
        PaymentStatus status,
        String failReason
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                info.id(),
                info.orderId(),
                info.orderNumber(),
                info.transactionId(),
                info.cardType(),
                info.cardNo(),
                info.amount(),
                info.status(),
                info.failReason()
            );
        }
    }
}
