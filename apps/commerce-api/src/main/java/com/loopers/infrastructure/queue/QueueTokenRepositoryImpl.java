package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueTokenRepository;
import com.loopers.domain.queue.TokenConsumeResult;
import com.loopers.domain.queue.TokenConsumeResult.ConsumeStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import org.springframework.data.redis.core.RedisCallback;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class QueueTokenRepositoryImpl implements QueueTokenRepository {

    private static final String KEY_PREFIX = "queue:";
    private static final String KEY_INFIX = ":token:";
    private static final String LUA_CONSUME_IF_MATCHES =
            "local val = redis.call('GET', KEYS[1]) " +
            "if val == false then return {'NOT_FOUND', '0'} end " +
            "if val ~= ARGV[1] then return {'MISMATCH', '0'} end " +
            "local ttl = redis.call('TTL', KEYS[1]) " +
            "redis.call('DEL', KEYS[1]) " +
            "return {'CONSUMED', tostring(ttl)}";

    @SuppressWarnings("rawtypes")
    private static final DefaultRedisScript<List> CONSUME_IF_MATCHES_SCRIPT =
            new DefaultRedisScript<>(LUA_CONSUME_IF_MATCHES, List.class);

    private final RedisTemplate<String, String> masterRedisTemplate;

    public QueueTokenRepositoryImpl(
            @Qualifier("masterRedisTemplate") RedisTemplate<String, String> masterRedisTemplate
    ) {
        this.masterRedisTemplate = masterRedisTemplate;
    }

    @Override
    public void save(String eventType, Long userId, String token, Duration ttl) {
        masterRedisTemplate.opsForValue().set(generateKey(eventType, userId), token, ttl);
    }

    @Override
    public void saveBatch(String eventType, Map<Long, String> userTokens, Duration ttl) {
        long ttlSeconds = ttl.toSeconds();
        masterRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (Map.Entry<Long, String> entry : userTokens.entrySet()) {
                byte[] key = generateKey(eventType, entry.getKey()).getBytes();
                byte[] value = entry.getValue().getBytes();
                connection.stringCommands().setEx(key, ttlSeconds, value);
            }
            return null;
        });
    }

    @Override
    public String get(String eventType, Long userId) {
        return masterRedisTemplate.opsForValue().get(generateKey(eventType, userId));
    }

    @Override
    public TokenConsumeResult consumeIfMatches(String eventType, Long userId, String expectedToken) {
        List<String> keys = Collections.singletonList(generateKey(eventType, userId));

        @SuppressWarnings("unchecked")
        List<String> result = masterRedisTemplate.execute(CONSUME_IF_MATCHES_SCRIPT, keys, expectedToken);

        if (result == null || result.size() < 2) {
            return new TokenConsumeResult(ConsumeStatus.NOT_FOUND, 0);
        }

        String status = result.get(0);
        return switch (status) {
            case "CONSUMED" -> new TokenConsumeResult(ConsumeStatus.CONSUMED, Math.max(Long.parseLong(result.get(1)), 0));
            case "MISMATCH" -> new TokenConsumeResult(ConsumeStatus.MISMATCH, 0);
            default -> new TokenConsumeResult(ConsumeStatus.NOT_FOUND, 0);
        };
    }

    private String generateKey(String eventType, Long userId) {
        return KEY_PREFIX + eventType + KEY_INFIX + userId;
    }
}
