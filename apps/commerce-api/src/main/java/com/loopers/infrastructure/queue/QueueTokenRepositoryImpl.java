package com.loopers.infrastructure.queue;

import com.loopers.domain.queue.QueueTokenRepository;
import com.loopers.domain.queue.TokenConsumeResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Repository
public class QueueTokenRepositoryImpl implements QueueTokenRepository {

    private static final String KEY_PREFIX = "queue:";
    private static final String KEY_INFIX = ":token:";
    private static final String LUA_GET_TTL_AND_DELETE =
            "local ttl = redis.call('TTL', KEYS[1]) " +
            "local val = redis.call('GETDEL', KEYS[1]) " +
            "if val == false then return {tostring(ttl), ''} end " +
            "return {tostring(ttl), val}";

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

    @Override
    public TokenConsumeResult getAndDeleteWithTtl(String eventType, Long userId) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>(LUA_GET_TTL_AND_DELETE, List.class);
        List<String> keys = Collections.singletonList(generateKey(eventType, userId));

        @SuppressWarnings("unchecked")
        List<String> result = masterRedisTemplate.execute(script, keys);

        if (result == null || result.size() < 2) {
            return new TokenConsumeResult(null, 0);
        }

        long ttl = Long.parseLong(result.get(0));
        String token = result.get(1);
        if (token == null || token.isEmpty()) {
            return new TokenConsumeResult(null, 0);
        }
        return new TokenConsumeResult(token, Math.max(ttl, 0));
    }

    private String generateKey(String eventType, Long userId) {
        return KEY_PREFIX + eventType + KEY_INFIX + userId;
    }
}
