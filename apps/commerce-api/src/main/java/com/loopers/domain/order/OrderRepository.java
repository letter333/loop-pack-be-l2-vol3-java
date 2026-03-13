package com.loopers.domain.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(Long id);

    Optional<Order> findByIdForUpdate(Long id);

    List<Order> findByMemberIdAndCreatedAtAfter(Long memberId, LocalDateTime startDate);

    List<Order> findByMemberId(Long memberId);

    List<Order> findAll();
}
