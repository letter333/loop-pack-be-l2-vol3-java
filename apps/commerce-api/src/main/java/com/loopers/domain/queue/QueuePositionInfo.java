package com.loopers.domain.queue;

public record QueuePositionInfo(
    QueuePositionStatus status,
    long position,
    long totalWaiting,
    long estimatedWaitSeconds,
    long suggestedPollIntervalMs,
    String token
) {
}
