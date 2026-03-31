package com.loopers.domain.queue;

public record TokenConsumeResult(
    String token,
    long ttlSeconds
) {
}
