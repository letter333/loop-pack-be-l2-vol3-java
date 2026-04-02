package com.loopers.domain.queue;

public record QueueInfo(
    long position,
    long totalWaiting
) {
}
