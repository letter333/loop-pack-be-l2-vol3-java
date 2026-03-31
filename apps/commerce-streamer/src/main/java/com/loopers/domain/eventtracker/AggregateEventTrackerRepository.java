package com.loopers.domain.eventtracker;

import java.time.ZonedDateTime;

public interface AggregateEventTrackerRepository {

    void upsert(String aggregateId, String eventType, ZonedDateTime eventCreatedAt);

    boolean isNewerEvent(String aggregateId, String eventType, ZonedDateTime eventCreatedAt);
}
