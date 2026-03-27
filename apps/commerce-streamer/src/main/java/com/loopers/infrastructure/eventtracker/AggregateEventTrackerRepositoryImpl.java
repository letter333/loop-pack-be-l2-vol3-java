package com.loopers.infrastructure.eventtracker;

import com.loopers.domain.eventtracker.AggregateEventTrackerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;

@Repository
@RequiredArgsConstructor
public class AggregateEventTrackerRepositoryImpl implements AggregateEventTrackerRepository {

    private final AggregateEventTrackerJpaRepository jpaRepository;

    @Override
    public void upsert(String aggregateId, String eventType, ZonedDateTime eventCreatedAt) {
        jpaRepository.upsert(aggregateId, eventType, eventCreatedAt);
    }

    @Override
    public boolean isNewerEvent(String aggregateId, String eventType, ZonedDateTime eventCreatedAt) {
        return jpaRepository.findByAggregateIdAndEventType(aggregateId, eventType)
            .map(tracker -> eventCreatedAt.isAfter(tracker.getLastEventCreatedAt()))
            .orElse(true);
    }
}
