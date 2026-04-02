package com.loopers.domain.queue;

public record TokenConsumeResult(
    ConsumeStatus status,
    long ttlSeconds
) {
    public enum ConsumeStatus {
        CONSUMED,
        NOT_FOUND,
        MISMATCH
    }
}
