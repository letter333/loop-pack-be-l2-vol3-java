package com.loopers.domain.queue;

public interface QueueRepository {

    boolean addIfAbsent(String eventType, Long userId, double score);

    Long getPosition(String eventType, Long userId);

    long getTotalWaiting(String eventType);
}
