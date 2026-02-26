package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Order> getOrders(Long memberId, LocalDateTime startDate) {
        if (memberId == null) {
            return orderRepository.findAll();
        }
        if (startDate == null) {
            return orderRepository.findByMemberId(memberId);
        }
        return orderRepository.findByMemberIdAndCreatedAtAfter(memberId, startDate);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Order cancelOrder(Long orderId) {
        Order order = getOrder(orderId);
        order.cancel();
        return orderRepository.save(order);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Order changeStatus(Long orderId, OrderStatus newStatus) {
        Order order = getOrder(orderId);
        order.transitionTo(newStatus);
        return orderRepository.save(order);
    }

    public void validateOwnership(Long memberId, Order order) {
        if (!order.isOwnedBy(memberId)) {
            throw new CoreException(ErrorType.FORBIDDEN, "해당 주문에 대한 권한이 없습니다.");
        }
    }
}
