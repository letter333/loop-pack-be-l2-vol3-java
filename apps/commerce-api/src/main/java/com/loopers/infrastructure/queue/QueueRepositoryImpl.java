package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
public class QueueRepositoryImpl implements QueueRepository {

    private static final String KEY_PREFIX = "queue:";
    private static final String KEY_SUFFIX = ":waiting";

    private final RedisTemplate<String, String> defaultRedisTemplate;
    private final RedisTemplate<String, String> masterRedisTemplate;

    public QueueRepositoryImpl(
            RedisTemplate<String, String> defaultRedisTemplate,
            @Qualifier("masterRedisTemplate") RedisTemplate<String, String> masterRedisTemplate
    ) {
        this.defaultRedisTemplate = defaultRedisTemplate;
        this.masterRedisTemplate = masterRedisTemplate;
    }

    @Override
    public boolean addIfAbsent(String eventType, Long userId, double score) {
        Boolean added = masterRedisTemplate.opsForZSet()
                .addIfAbsent(generateKey(eventType), String.valueOf(userId), score);
        return Boolean.TRUE.equals(added);
    }

    @Override
    public Long getPosition(String eventType, Long userId) {
        Long rank = defaultRedisTemplate.opsForZSet()
                .rank(generateKey(eventType), String.valueOf(userId));
        return rank != null ? rank + 1 : null;
    }

    @Override
    public long getTotalWaiting(String eventType) {
        Long size = defaultRedisTemplate.opsForZSet().zCard(generateKey(eventType));
        return size != null ? size : 0L;
    }

    @Override
    public List<Long> popMin(String eventType, int count) {
        Set<ZSetOperations.TypedTuple<String>> tuples =
                masterRedisTemplate.opsForZSet().popMin(generateKey(eventType), count);
        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }
        return tuples.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .toList();
    }

    private String generateKey(String eventType) {
        return KEY_PREFIX + eventType + KEY_SUFFIX;
    }
}
