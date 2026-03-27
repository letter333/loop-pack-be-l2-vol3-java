package com.loopers.application.payment;

import com.loopers.application.event.PaymentSuccessEvent;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final MemberService memberService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final TransactionTemplate transactionTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    public PaymentInfo requestPayment(String loginId, String password, PaymentCommand.Create command) {
        // CB OPEN 시 Fast Fail — Payment 생성 안 함
        if (!paymentGateway.isAvailable()) {
            throw new CoreException(ErrorType.SERVICE_UNAVAILABLE, "결제 시스템이 일시적으로 이용 불가합니다.");
        }

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

        if (command.isSuccess()) {
            payment.markSuccess();
            Payment saved = paymentService.save(payment);
            orderService.changeStatus(payment.getOrderId(), OrderStatus.PAID);

            // 부가 로직: 결제 성공 이벤트 발행 (유저 행동 로깅용)
            applicationEventPublisher.publishEvent(new PaymentSuccessEvent(
                saved.getId(), saved.getOrderId(), saved.getMemberId(), saved.getAmount()
            ));

            return PaymentInfo.from(saved);
        } else {
            payment.markFailed(command.message());
            Payment saved = paymentService.save(payment);
            return PaymentInfo.from(saved);
        }
    }

    public PaymentInfo recoverPayment(String loginId, String password, Long paymentId) {
        Member member = memberService.authenticate(loginId, password);
        Payment payment = paymentService.getPayment(paymentId);
        validatePaymentOwnership(member.getId(), payment);

        return doRecover(payment);
    }

    public PaymentInfo recoverPayment(Payment payment) {
        Payment freshPayment = paymentService.getPayment(payment.getId());
        return doRecover(freshPayment);
    }

    private PaymentInfo doRecover(Payment payment) {
        if (payment.isTerminal()) {
            return PaymentInfo.from(payment);
        }

        PaymentGatewayResponse pgResponse;
        try {
            if (payment.getTransactionId() != null) {
                pgResponse = paymentGateway.getPaymentStatus(
                    payment.getTransactionId(), payment.getMemberId());
            } else {
                pgResponse = paymentGateway.getPaymentByOrderId(
                    payment.getOrderNumber(), payment.getMemberId());
            }
        } catch (PaymentGatewayException e) {
            log.warn("결제 복구 중 PG 조회 실패: paymentId={}, error={}", payment.getId(), e.getMessage());
            return PaymentInfo.from(payment);
        }

        return applyRecoveryResult(payment, pgResponse);
    }

    private PaymentInfo applyRecoveryResult(Payment payment, PaymentGatewayResponse pgResponse) {
        if (pgResponse.isSuccess()) {
            if (payment.getTransactionId() == null) {
                payment.assignTransactionId(pgResponse.transactionId());
            }
            payment.markSuccess();
            return transactionTemplate.execute(status -> {
                Payment saved = paymentService.save(payment);
                orderService.changeStatus(payment.getOrderId(), OrderStatus.PAID);
                return PaymentInfo.from(saved);
            });
        } else if (pgResponse.isFailed()) {
            payment.markFailed(pgResponse.message());
            return PaymentInfo.from(paymentService.save(payment));
        }

        log.warn("PG 알 수 없는 상태: paymentId={}, pgStatus={}", payment.getId(), pgResponse.status());
        return PaymentInfo.from(payment);
    }

    private void validatePaymentOwnership(Long memberId, Payment payment) {
        if (!memberId.equals(payment.getMemberId())) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 결제에 대한 권한이 없습니다.");
        }
    }
}
