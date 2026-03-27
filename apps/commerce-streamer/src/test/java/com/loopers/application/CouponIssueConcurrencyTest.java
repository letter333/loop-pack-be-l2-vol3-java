package com.loopers.application;

import com.loopers.infrastructure.coupon.CouponIssueEntity;
import com.loopers.infrastructure.coupon.CouponIssueJpaRepository;
import com.loopers.infrastructure.coupon.MemberCouponIssueJpaRepository;
import com.loopers.infrastructure.redis.CouponIssueRedisRepository;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("선착순 쿠폰 발급 동시성 테스트")
class CouponIssueConcurrencyTest {

    @Autowired
    private CouponIssueService couponIssueService;

    @Autowired
    private CouponIssueJpaRepository couponIssueJpaRepository;

    @Autowired
    private MemberCouponIssueJpaRepository memberCouponIssueJpaRepository;

    @Autowired
    private CouponIssueRedisRepository couponIssueRedisRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @Test
    @DisplayName("총 수량 50개 쿠폰에 100명이 동시 요청하면 정확히 50개만 발급된다")
    @Sql(statements = {
        "INSERT INTO coupons (name, coupon_type, discount_value, min_order_amount, total_quantity, issued_quantity, valid_from, valid_until, created_at, updated_at) " +
        "VALUES ('동시성테스트쿠폰', 'FIXED', 1000, 0, 50, 0, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, NOW(), NOW())"
    })
    void concurrentIssue_exactlyTotalQuantityIssued() throws InterruptedException {
        // Arrange
        int totalQuantity = 50;
        int threadCount = 100;

        CouponIssueEntity coupon = couponIssueJpaRepository.findAll().get(0);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exhaustedCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            long memberId = i + 1L;
            executorService.submit(() -> {
                String requestId = UUID.randomUUID().toString();
                try {
                    couponIssueService.processCouponIssue(requestId, memberId, couponId);
                    var status = couponIssueRedisRepository.getRequestStatus(requestId);
                    if (status.isPresent() && "SUCCESS".equals(status.get())) {
                        successCount.incrementAndGet();
                    } else if (status.isPresent() && "EXHAUSTED".equals(status.get())) {
                        exhaustedCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        CouponIssueEntity result = couponIssueJpaRepository.findById(couponId).orElseThrow();
        long issuedMemberCoupons = memberCouponIssueJpaRepository.count();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(totalQuantity),
            () -> assertThat(exhaustedCount.get()).isEqualTo(threadCount - totalQuantity),
            () -> assertThat(result.getIssuedQuantity()).isEqualTo(totalQuantity),
            () -> assertThat(issuedMemberCoupons).isEqualTo(totalQuantity)
        );
    }

    @Test
    @DisplayName("동일 회원이 같은 쿠폰을 동시에 요청하면 1개만 발급된다")
    @Sql(statements = {
        "INSERT INTO coupons (name, coupon_type, discount_value, min_order_amount, total_quantity, issued_quantity, valid_from, valid_until, created_at, updated_at) " +
        "VALUES ('중복테스트쿠폰', 'FIXED', 1000, 0, 100, 0, NOW() - INTERVAL 1 DAY, NOW() + INTERVAL 30 DAY, NOW(), NOW())"
    })
    void concurrentIssue_sameUser_onlyOneIssued() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Long memberId = 999L;

        CouponIssueEntity coupon = couponIssueJpaRepository.findAll().get(0);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                String requestId = UUID.randomUUID().toString();
                try {
                    couponIssueService.processCouponIssue(requestId, memberId, couponId);
                    var status = couponIssueRedisRepository.getRequestStatus(requestId);
                    if (status.isPresent() && "SUCCESS".equals(status.get())) {
                        successCount.incrementAndGet();
                    } else if (status.isPresent() && "DUPLICATE".equals(status.get())) {
                        duplicateCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        long issuedMemberCoupons = memberCouponIssueJpaRepository.count();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(duplicateCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(issuedMemberCoupons).isEqualTo(1)
        );
    }
}
