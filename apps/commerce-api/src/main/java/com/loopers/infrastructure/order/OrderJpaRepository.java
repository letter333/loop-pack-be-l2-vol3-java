package com.loopers.infrastructure.order;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<OrderEntity> findByMemberIdAndCreatedAtAfterOrderByCreatedAtDesc(Long memberId, LocalDateTime startDate);

    List<OrderEntity> findAllByOrderByCreatedAtDesc();
}
