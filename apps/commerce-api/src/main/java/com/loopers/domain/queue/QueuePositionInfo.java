package com.loopers.domain.queue;

public record QueuePositionInfo(
    String status,
    long position,
    long totalWaiting,
    long estimatedWaitSeconds,
    long suggestedPollIntervalMs,
    String token
) {
}
