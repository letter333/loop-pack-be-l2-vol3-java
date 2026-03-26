package com.loopers.infrastructure.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Repository
public class CouponIssueRedisRepository {

    private static final String ISSUED_COUNT_KEY = "coupon:issued:";
    private static final String TOTAL_QUANTITY_KEY = "coupon:total:";
    private static final String REQUEST_STATUS_KEY = "coupon:issue-request:";
    private static final long REQUEST_STATUS_TTL_SECONDS = 86400; // 24h

    private final RedisTemplate<String, String> masterRedisTemplate;

    public CouponIssueRedisRepository(
        @Qualifier("redisTemplateMaster") RedisTemplate<String, String> masterRedisTemplate
    ) {
        this.masterRedisTemplate = masterRedisTemplate;
    }

    public Long incrementIssuedCount(Long couponId) {
        return masterRedisTemplate.opsForValue().increment(ISSUED_COUNT_KEY + couponId);
    }

    public void decrementIssuedCount(Long couponId) {
        masterRedisTemplate.opsForValue().decrement(ISSUED_COUNT_KEY + couponId);
    }

    public void initIssuedCountIfAbsent(Long couponId, long currentCount, LocalDateTime validUntil) {
        String key = ISSUED_COUNT_KEY + couponId;
        Boolean wasSet = masterRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(currentCount));
        if (Boolean.TRUE.equals(wasSet)) {
            setTtlByValidUntil(key, validUntil);
        }
    }

    public void setTotalQuantity(Long couponId, int totalQuantity, LocalDateTime validUntil) {
        String key = TOTAL_QUANTITY_KEY + couponId;
        masterRedisTemplate.opsForValue().set(key, String.valueOf(totalQuantity));
        setTtlByValidUntil(key, validUntil);
    }

    public Optional<Integer> getTotalQuantity(Long couponId) {
        String value = masterRedisTemplate.opsForValue().get(TOTAL_QUANTITY_KEY + couponId);
        return Optional.ofNullable(value).map(Integer::parseInt);
    }

    public void setRequestStatus(String requestId, String status) {
        masterRedisTemplate.opsForValue().set(
            REQUEST_STATUS_KEY + requestId,
            status,
            REQUEST_STATUS_TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    public Optional<String> getRequestStatus(String requestId) {
        String value = masterRedisTemplate.opsForValue().get(REQUEST_STATUS_KEY + requestId);
        return Optional.ofNullable(value);
    }

    private void setTtlByValidUntil(String key, LocalDateTime validUntil) {
        LocalDateTime expireAt = validUntil.plusDays(1);
        Duration ttl = Duration.between(LocalDateTime.now(), expireAt);
        if (!ttl.isNegative()) {
            masterRedisTemplate.expire(key, ttl.getSeconds(), TimeUnit.SECONDS);
        }
    }
}
