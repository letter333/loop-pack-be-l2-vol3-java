package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueTokenRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Repository
public class QueueTokenRepositoryImpl implements QueueTokenRepository {

    private static final String KEY_PREFIX = "queue:";
    private static final String KEY_INFIX = ":token:";

    private final RedisTemplate<String, String> defaultRedisTemplate;
    private final RedisTemplate<String, String> masterRedisTemplate;

    public QueueTokenRepositoryImpl(
            RedisTemplate<String, String> defaultRedisTemplate,
            @Qualifier("masterRedisTemplate") RedisTemplate<String, String> masterRedisTemplate
    ) {
        this.defaultRedisTemplate = defaultRedisTemplate;
        this.masterRedisTemplate = masterRedisTemplate;
    }

    @Override
    public void save(String eventType, Long userId, String token, Duration ttl) {
        masterRedisTemplate.opsForValue().set(generateKey(eventType, userId), token, ttl);
    }

    @Override
    public String getAndDelete(String eventType, Long userId) {
        return masterRedisTemplate.opsForValue().getAndDelete(generateKey(eventType, userId));
    }

    @Override
    public String get(String eventType, Long userId) {
        return defaultRedisTemplate.opsForValue().get(generateKey(eventType, userId));
    }

    @Override
    public Long getExpire(String eventType, Long userId) {
        return defaultRedisTemplate.getExpire(generateKey(eventType, userId), TimeUnit.SECONDS);
    }

    private String generateKey(String eventType, Long userId) {
        return KEY_PREFIX + eventType + KEY_INFIX + userId;
    }
}
