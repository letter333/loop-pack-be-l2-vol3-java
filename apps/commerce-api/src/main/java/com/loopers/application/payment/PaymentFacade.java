package com.loopers.application.payment;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import com.loopers.domain.payment.PaymentService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final MemberService memberService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;

    public PaymentInfo requestPayment(String loginId, String password, PaymentCommand.Create command) {
        // TX1: 인증 + 주문 조회 + Payment 생성/저장
        Member member = memberService.authenticate(loginId, password);
        Order order = orderService.getOrder(command.orderId());
        orderService.validateOwnership(member.getId(), order);

        Payment payment = new Payment(
            order.getId(), member.getId(), order.getOrderNumber(),
            command.cardType(), command.cardNo(), order.getPaymentAmount()
        );
        Payment savedPayment = paymentService.save(payment);

        // PG 호출 (트랜잭션 없음)
        try {
            PaymentGatewayResponse response = paymentGateway.requestPayment(
                order.getOrderNumber(), command.cardType(), command.cardNo(),
                order.getPaymentAmount(), member.getId()
            );
            savedPayment.assignTransactionId(response.transactionId());
        } catch (PaymentGatewayException e) {
            if (e.isTimeout()) {
                savedPayment.markTimeout();
            } else {
                savedPayment.markFailed(e.getMessage());
            }
        }

        // TX2: 상태 업데이트 저장
        Payment updatedPayment = paymentService.save(savedPayment);
        return PaymentInfo.from(updatedPayment);
    }

    @Transactional
    public PaymentInfo handleCallback(PaymentCommand.Callback command) {
        Payment payment = paymentService.getPaymentByTransactionId(command.transactionId());

        if (payment.isTerminal()) {
            return PaymentInfo.from(payment);
        }

        payment.receiveCallback();

        if ("SUCCESS".equals(command.status())) {
            payment.markSuccess();
            Payment saved = paymentService.save(payment);
            orderService.changeStatus(payment.getOrderId(), OrderStatus.PAID);
            return PaymentInfo.from(saved);
        } else {
            payment.markFailed(command.message());
            Payment saved = paymentService.save(payment);
            return PaymentInfo.from(saved);
        }
    }

    @Transactional
    public PaymentInfo recoverPayment(String loginId, String password, Long paymentId) {
        Member member = memberService.authenticate(loginId, password);
        Payment payment = paymentService.getPayment(paymentId);
        validatePaymentOwnership(member.getId(), payment);

        return doRecover(payment);
    }

    @Transactional
    public PaymentInfo recoverPayment(Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return doRecover(payment);
    }

    private PaymentInfo doRecover(Payment payment) {
        if (payment.isTerminal()) {
            return PaymentInfo.from(payment);
        }

        PaymentGatewayResponse pgResponse;
        if (payment.getTransactionId() != null) {
            pgResponse = paymentGateway.getPaymentStatus(
                payment.getTransactionId(), payment.getMemberId());
        } else {
            pgResponse = paymentGateway.getPaymentByOrderId(
                payment.getOrderNumber(), payment.getMemberId());
        }

        if ("SUCCESS".equals(pgResponse.status())) {
            if (payment.getTransactionId() == null) {
                payment.assignTransactionId(pgResponse.transactionId());
            }
            payment.markSuccess();
            Payment saved = paymentService.save(payment);
            orderService.changeStatus(payment.getOrderId(), OrderStatus.PAID);
            return PaymentInfo.from(saved);
        } else if ("FAILED".equals(pgResponse.status())) {
            payment.markFailed(pgResponse.message());
            return PaymentInfo.from(paymentService.save(payment));
        }

        return PaymentInfo.from(payment);
    }

    private void validatePaymentOwnership(Long memberId, Payment payment) {
        if (!memberId.equals(payment.getMemberId())) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 결제에 대한 권한이 없습니다.");
        }
    }
}
