package com.loopers.domain.eventtracker;

import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
public class AggregateEventTracker {

    private final String aggregateId;
    private final String eventType;
    private final ZonedDateTime lastEventCreatedAt;

    public AggregateEventTracker(String aggregateId, String eventType, ZonedDateTime lastEventCreatedAt) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.lastEventCreatedAt = lastEventCreatedAt;
    }
}
