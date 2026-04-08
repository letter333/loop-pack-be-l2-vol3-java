package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.ProductRankingInfo;
import com.loopers.domain.ranking.ProductRankingRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Repository
public class ProductRankingRedisRepository implements ProductRankingRepository {

    private static final String KEY_PREFIX = "ranking:all:";

    private final RedisTemplate<String, String> defaultRedisTemplate;

    public ProductRankingRedisRepository(RedisTemplate<String, String> defaultRedisTemplate) {
        this.defaultRedisTemplate = defaultRedisTemplate;
    }

    @Override
    public List<ProductRankingInfo> getRankings(String dateKey, int offset, int size) {
        String key = KEY_PREFIX + dateKey;
        Set<ZSetOperations.TypedTuple<String>> tuples =
            defaultRedisTemplate.opsForZSet().reverseRangeWithScores(key, offset, (long) offset + size - 1);

        if (tuples == null || tuples.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProductRankingInfo> rankings = new ArrayList<>();
        int index = 0;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            rankings.add(new ProductRankingInfo(
                Long.parseLong(tuple.getValue()),
                tuple.getScore(),
                (long) offset + index + 1
            ));
            index++;
        }
        return rankings;
    }

    @Override
    public Long getRank(String dateKey, Long productId) {
        String key = KEY_PREFIX + dateKey;
        Long rank = defaultRedisTemplate.opsForZSet().reverseRank(key, String.valueOf(productId));
        return rank != null ? rank + 1 : null;
    }

    @Override
    public Double getScore(String dateKey, Long productId) {
        String key = KEY_PREFIX + dateKey;
        return defaultRedisTemplate.opsForZSet().score(key, String.valueOf(productId));
    }

    @Override
    public Long getTotalCount(String dateKey) {
        String key = KEY_PREFIX + dateKey;
        Long count = defaultRedisTemplate.opsForZSet().zCard(key);
        return count != null ? count : 0L;
    }
}
