package com.loopers.infrastructure.eventhandled;

import com.loopers.domain.eventhandled.EventHandledRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class EventHandledRepositoryImpl implements EventHandledRepository {

    private final EventHandledJpaRepository eventHandledJpaRepository;

    @Override
    public boolean existsByEventId(String eventId) {
        return eventHandledJpaRepository.existsById(eventId);
    }

    @Override
    public void save(String eventId) {
        eventHandledJpaRepository.save(EventHandledEntity.of(eventId));
    }
}
