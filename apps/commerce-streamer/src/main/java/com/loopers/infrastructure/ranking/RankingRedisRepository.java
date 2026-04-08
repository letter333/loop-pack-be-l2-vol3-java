package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.RankingRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class RankingRedisRepository implements RankingRepository {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final long TTL_DAYS = 2;

    private final RedisTemplate<String, String> masterRedisTemplate;

    public RankingRedisRepository(
        @Qualifier("redisTemplateMaster") RedisTemplate<String, String> masterRedisTemplate
    ) {
        this.masterRedisTemplate = masterRedisTemplate;
    }

    @Override
    public void incrementScore(String dateKey, Long productId, double score) {
        String key = KEY_PREFIX + dateKey;
        masterRedisTemplate.opsForZSet().incrementScore(key, String.valueOf(productId), score);
        masterRedisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    @Override
    public void carryOverScores(String fromDateKey, String toDateKey, double weight) {
        String fromKey = KEY_PREFIX + fromDateKey;
        String toKey = KEY_PREFIX + toDateKey;

        Set<ZSetOperations.TypedTuple<String>> tuples =
            masterRedisTemplate.opsForZSet().rangeWithScores(fromKey, 0, -1);

        if (tuples != null && !tuples.isEmpty()) {
            for (ZSetOperations.TypedTuple<String> tuple : tuples) {
                masterRedisTemplate.opsForZSet().incrementScore(
                    toKey, tuple.getValue(), tuple.getScore() * weight
                );
            }
        }

        masterRedisTemplate.expire(toKey, TTL_DAYS, TimeUnit.DAYS);
    }
}
