package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Payment {

    private Long id;
    private Long orderId;
    private Long memberId;
    private String orderNumber;
    private String transactionId;
    private String cardType;
    private String cardNo;
    private Long amount;
    private PaymentStatus status;
    private String failReason;
    private LocalDateTime callbackReceivedAt;

    public Payment(Long orderId, Long memberId, String orderNumber,
                   String cardType, String cardNo, Long amount) {
        validateOrderId(orderId);
        validateMemberId(memberId);
        validateAmount(amount);
        validateCardType(cardType);
        validateCardNo(cardNo);

        this.orderId = orderId;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.status = PaymentStatus.REQUESTED;
    }

    public Payment(Long id, Long orderId, Long memberId, String orderNumber,
                   String transactionId, String cardType, String cardNo,
                   Long amount, PaymentStatus status, String failReason,
                   LocalDateTime callbackReceivedAt) {
        this.id = id;
        this.orderId = orderId;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.transactionId = transactionId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.status = status;
        this.failReason = failReason;
        this.callbackReceivedAt = callbackReceivedAt;
    }

    public void assignTransactionId(String transactionId) {
        if (this.transactionId != null) {
            throw new CoreException(ErrorType.CONFLICT, "이미 transactionId가 할당되어 있습니다.");
        }
        this.transactionId = transactionId;
    }

    public void markSuccess() {
        validateNotTerminal();
        this.status = PaymentStatus.SUCCESS;
    }

    public void markFailed(String reason) {
        validateNotTerminal();
        this.status = PaymentStatus.FAILED;
        this.failReason = reason;
    }

    public void markTimeout() {
        if (this.status != PaymentStatus.REQUESTED) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("'%s' 상태에서 TIMEOUT으로 변경할 수 없습니다.", this.status));
        }
        this.status = PaymentStatus.TIMEOUT;
    }

    public void receiveCallback() {
        this.callbackReceivedAt = LocalDateTime.now();
    }

    public boolean isTerminal() {
        return this.status == PaymentStatus.SUCCESS || this.status == PaymentStatus.FAILED;
    }

    private void validateNotTerminal() {
        if (isTerminal()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("이미 최종 상태('%s')인 결제는 상태를 변경할 수 없습니다.", this.status));
        }
    }

    private void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
        }
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private void validateAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }

    private void validateCardType(String cardType) {
        if (cardType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 종류는 필수입니다.");
        }
    }

    private void validateCardNo(String cardNo) {
        if (cardNo == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수입니다.");
        }
    }
}
