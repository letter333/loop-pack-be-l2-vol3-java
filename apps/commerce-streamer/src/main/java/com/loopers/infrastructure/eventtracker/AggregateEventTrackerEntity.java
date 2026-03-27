package com.loopers.infrastructure.eventtracker;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(name = "aggregate_event_tracker")
@IdClass(AggregateEventTrackerId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AggregateEventTrackerEntity {

    @Id
    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Id
    @Column(name = "event_type", length = 50)
    private String eventType;

    @Column(name = "last_event_created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private ZonedDateTime lastEventCreatedAt;

    public static AggregateEventTrackerEntity of(String aggregateId, String eventType, ZonedDateTime lastEventCreatedAt) {
        AggregateEventTrackerEntity entity = new AggregateEventTrackerEntity();
        entity.aggregateId = aggregateId;
        entity.eventType = eventType;
        entity.lastEventCreatedAt = lastEventCreatedAt;
        return entity;
    }
}
