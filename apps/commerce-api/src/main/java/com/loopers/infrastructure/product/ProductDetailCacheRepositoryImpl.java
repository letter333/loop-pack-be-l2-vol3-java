package com.loopers.infrastructure.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductDetailCacheRepository;
import com.loopers.application.product.ProductDetailInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Repository
public class ProductDetailCacheRepositoryImpl implements ProductDetailCacheRepository {

    private static final String KEY_PREFIX = "product:detail:";
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisTemplate<String, String> defaultRedisTemplate;
    private final RedisTemplate<String, String> masterRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductDetailCacheRepositoryImpl(
            RedisTemplate<String, String> defaultRedisTemplate,
            @Qualifier("masterRedisTemplate") RedisTemplate<String, String> masterRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.defaultRedisTemplate = defaultRedisTemplate;
        this.masterRedisTemplate = masterRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<ProductDetailInfo> get(Long productId) {
        try {
            String json = defaultRedisTemplate.opsForValue().get(generateKey(productId));
            if (json == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, ProductDetailInfo.class));
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패: productId={}", productId, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(Long productId, ProductDetailInfo productDetailInfo) {
        try {
            String json = objectMapper.writeValueAsString(productDetailInfo);
            masterRedisTemplate.opsForValue().set(generateKey(productId), json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Redis 캐시 저장 실패: productId={}", productId, e);
        }
    }

    @Override
    public void evict(Long productId) {
        try {
            masterRedisTemplate.delete(generateKey(productId));
        } catch (Exception e) {
            log.warn("Redis 캐시 삭제 실패: productId={}", productId, e);
        }
    }

    private String generateKey(Long productId) {
        return KEY_PREFIX + productId;
    }
}
