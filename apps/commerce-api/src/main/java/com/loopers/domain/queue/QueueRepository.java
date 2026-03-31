package com.loopers.domain.queue;

import java.util.List;

public interface QueueRepository {

    boolean addIfAbsent(String eventType, Long userId, double score);

    Long getPosition(String eventType, Long userId);

    long getTotalWaiting(String eventType);

    List<Long> popMin(String eventType, int count);
}
