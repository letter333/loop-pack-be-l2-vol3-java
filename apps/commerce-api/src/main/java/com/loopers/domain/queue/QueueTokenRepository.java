package com.loopers.domain.queue;

import java.time.Duration;

public interface QueueTokenRepository {

    void save(String eventType, Long userId, String token, Duration ttl);

    String getAndDelete(String eventType, Long userId);

    String get(String eventType, Long userId);

    Long getExpire(String eventType, Long userId);

    TokenConsumeResult getAndDeleteWithTtl(String eventType, Long userId);
}
