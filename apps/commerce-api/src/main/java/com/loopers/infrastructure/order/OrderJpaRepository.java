package com.loopers.infrastructure.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {

    @Query("SELECT o FROM OrderEntity o LEFT JOIN FETCH o.orderProducts WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithOrderProducts(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.orderProducts WHERE o.memberId = :memberId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByMemberIdWithOrderProducts(@Param("memberId") Long memberId);

    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.orderProducts WHERE o.memberId = :memberId AND o.createdAt > :startDate ORDER BY o.createdAt DESC")
    List<OrderEntity> findByMemberIdAndCreatedAtAfterWithOrderProducts(@Param("memberId") Long memberId, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT DISTINCT o FROM OrderEntity o LEFT JOIN FETCH o.orderProducts ORDER BY o.createdAt DESC")
    List<OrderEntity> findAllWithOrderProducts();
}
