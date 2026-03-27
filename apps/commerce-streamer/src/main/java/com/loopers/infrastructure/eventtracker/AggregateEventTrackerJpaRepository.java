package com.loopers.infrastructure.eventtracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.Optional;

public interface AggregateEventTrackerJpaRepository extends JpaRepository<AggregateEventTrackerEntity, AggregateEventTrackerId> {

    @Modifying
    @Query(value = """
        INSERT INTO aggregate_event_tracker (aggregate_id, event_type, last_event_created_at)
        VALUES (:aggregateId, :eventType, :eventCreatedAt)
        ON DUPLICATE KEY UPDATE last_event_created_at = GREATEST(last_event_created_at, :eventCreatedAt)
        """, nativeQuery = true)
    void upsert(
        @Param("aggregateId") String aggregateId,
        @Param("eventType") String eventType,
        @Param("eventCreatedAt") ZonedDateTime eventCreatedAt
    );

    @Query("SELECT t FROM AggregateEventTrackerEntity t WHERE t.aggregateId = :aggregateId AND t.eventType = :eventType")
    Optional<AggregateEventTrackerEntity> findByAggregateIdAndEventType(
        @Param("aggregateId") String aggregateId,
        @Param("eventType") String eventType
    );
}
