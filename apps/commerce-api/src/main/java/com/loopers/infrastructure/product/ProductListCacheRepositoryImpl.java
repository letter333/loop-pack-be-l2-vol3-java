package com.loopers.infrastructure.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.product.ProductListCacheRepository;
import com.loopers.domain.product.ProductSortType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Repository
public class ProductListCacheRepositoryImpl implements ProductListCacheRepository {

    private static final String KEY_PREFIX = "product:list:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> defaultRedisTemplate;
    private final RedisTemplate<String, String> masterRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductListCacheRepositoryImpl(
            RedisTemplate<String, String> defaultRedisTemplate,
            @Qualifier("masterRedisTemplate") RedisTemplate<String, String> masterRedisTemplate,
            ObjectMapper objectMapper
    ) {
        this.defaultRedisTemplate = defaultRedisTemplate;
        this.masterRedisTemplate = masterRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Page<ProductInfo>> get(String cacheKey) {
        try {
            String json = defaultRedisTemplate.opsForValue().get(cacheKey);
            if (json == null) {
                return Optional.empty();
            }
            CachedPage<ProductInfo> cachedPage = objectMapper.readValue(json,
                    new TypeReference<CachedPage<ProductInfo>>() {});
            Page<ProductInfo> page = new PageImpl<>(
                    cachedPage.content(),
                    PageRequest.of(cachedPage.page(), cachedPage.size()),
                    cachedPage.totalElements()
            );
            return Optional.of(page);
        } catch (Exception e) {
            log.warn("Redis 목록 캐시 조회 실패: cacheKey={}", cacheKey, e);
            return Optional.empty();
        }
    }

    @Override
    public void put(String cacheKey, Page<ProductInfo> page) {
        try {
            CachedPage<ProductInfo> cachedPage = new CachedPage<>(
                    page.getContent(),
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements()
            );
            String json = objectMapper.writeValueAsString(cachedPage);
            masterRedisTemplate.opsForValue().set(cacheKey, json, TTL);
        } catch (JsonProcessingException e) {
            log.warn("Redis 목록 캐시 저장 실패: cacheKey={}", cacheKey, e);
        }
    }

    @Override
    public void evict(String cacheKey) {
        try {
            masterRedisTemplate.delete(cacheKey);
        } catch (Exception e) {
            log.warn("Redis 목록 캐시 삭제 실패: cacheKey={}", cacheKey, e);
        }
    }

    @Override
    public void evictAll() {
        try {
            Set<String> keys = new HashSet<>();
            ScanOptions scanOptions = ScanOptions.scanOptions()
                    .match(KEY_PREFIX + "*")
                    .count(100)
                    .build();
            try (Cursor<String> cursor = masterRedisTemplate.scan(scanOptions)) {
                while (cursor.hasNext()) {
                    keys.add(cursor.next());
                }
            }
            if (!keys.isEmpty()) {
                masterRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("Redis 목록 캐시 전체 삭제 실패", e);
        }
    }

    @Override
    public String generateKey(Long categoryId, Long brandId, ProductSortType sort, int page, int size) {
        return KEY_PREFIX
                + "c" + (categoryId != null ? categoryId : 0)
                + ":b" + (brandId != null ? brandId : 0)
                + ":s" + sort.name()
                + ":p" + page
                + ":sz" + size;
    }
}
