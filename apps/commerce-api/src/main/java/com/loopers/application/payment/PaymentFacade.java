package com.loopers.application.payment;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentGatewayException;
import com.loopers.domain.payment.PaymentGatewayResponse;
import com.loopers.domain.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
