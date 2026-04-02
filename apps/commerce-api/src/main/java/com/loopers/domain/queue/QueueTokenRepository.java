package com.loopers.domain.queue;

import java.time.Duration;
import java.util.Map;

public interface QueueTokenRepository {

    void save(String eventType, Long userId, String token, Duration ttl);

    void saveBatch(String eventType, Map<Long, String> userTokens, Duration ttl);

    String get(String eventType, Long userId);

    TokenConsumeResult consumeIfMatches(String eventType, Long userId, String expectedToken);
}
