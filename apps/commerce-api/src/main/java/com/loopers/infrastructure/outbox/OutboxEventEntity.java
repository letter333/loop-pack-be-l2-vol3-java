package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_unpublished", columnList = "published_at, created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "published_at")
    private ZonedDateTime publishedAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = ZonedDateTime.now();
    }

    public static OutboxEventEntity from(OutboxEvent event) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.aggregateType = event.getAggregateType();
        entity.aggregateId = event.getAggregateId();
        entity.eventType = event.getEventType();
        entity.payload = event.getPayload();
        return entity;
    }

    public OutboxEvent toDomain() {
        return new OutboxEvent(
            id,
            aggregateType,
            aggregateId,
            eventType,
            payload,
            createdAt != null ? createdAt.toLocalDateTime() : null,
            publishedAt != null ? publishedAt.toLocalDateTime() : null
        );
    }

    public void markPublished() {
        this.publishedAt = ZonedDateTime.now();
    }
}
