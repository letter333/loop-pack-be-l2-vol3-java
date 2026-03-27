package com.loopers.infrastructure.coupon;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class CouponIssueStatusRedisRepository {

    private static final String REQUEST_STATUS_KEY = "coupon:issue-request:";

    private final RedisTemplate<String, String> redisTemplate;

    public CouponIssueStatusRedisRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Optional<String> getRequestStatus(String requestId) {
        String value = redisTemplate.opsForValue().get(REQUEST_STATUS_KEY + requestId);
        return Optional.ofNullable(value);
    }
}
