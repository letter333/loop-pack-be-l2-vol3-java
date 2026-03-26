package com.loopers.domain.eventhandled;

public interface EventHandledRepository {

    boolean existsByEventId(String eventId);

    void save(String eventId);
}
