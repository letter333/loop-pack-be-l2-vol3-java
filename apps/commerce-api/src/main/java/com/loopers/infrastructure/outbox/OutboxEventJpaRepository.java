package com.loopers.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at ASC LIMIT :limit",
           nativeQuery = true)
    List<OutboxEventEntity> findUnpublished(@Param("limit") int limit);

    @Modifying
    @Query(value = "UPDATE outbox_events SET published_at = NOW() WHERE id = :id", nativeQuery = true)
    void markPublished(@Param("id") Long id);
}
