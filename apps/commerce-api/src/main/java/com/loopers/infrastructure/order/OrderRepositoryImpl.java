package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        OrderEntity entity;
        if (order.getId() != null) {
            entity = orderJpaRepository.findById(order.getId())
                .orElseGet(() -> OrderEntity.from(order));
            entity.update(order.getStatus());
        } else {
            entity = OrderEntity.from(order);
        }
        return orderJpaRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id)
            .map(OrderEntity::toDomain);
    }

    @Override
    public List<Order> findByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime startDate) {
        return orderJpaRepository.findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(memberId, startDate)
            .stream()
            .map(OrderEntity::toDomain)
            .toList();
    }

    @Override
    public List<Order> findByMemberId(Long memberId) {
        return orderJpaRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .stream()
            .map(OrderEntity::toDomain)
            .toList();
    }

    @Override
    public List<Order> findAll() {
        return orderJpaRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(OrderEntity::toDomain)
            .toList();
    }
}
