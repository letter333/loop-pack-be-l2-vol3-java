package com.loopers.domain.coupon;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("쿠폰 발급 동시성 테스트")
class CouponIssueConcurrencyTest {

    @Autowired
    private MemberCouponService memberCouponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동시에 쿠폰을 발급해도 정확한 발급 수량이 유지된다")
    void issueCoupon_concurrently_maintainsCorrectIssuedQuantity() throws InterruptedException {
        // Arrange
        int totalQuantity = 100;
        int threadCount = 10;

        Coupon coupon = saveCoupon(totalQuantity);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            Long memberId = saveMember("user" + i).getId();
            executorService.submit(() -> {
                try {
                    memberCouponService.issueCoupon(memberId, couponId);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        Coupon result = couponRepository.findById(couponId).orElseThrow();
        assertThat(result.getIssuedQuantity()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("총 수량보다 많은 동시 요청 시 정확히 총 수량만큼만 발급된다")
    void issueCoupon_concurrently_preventsOverIssuance() throws InterruptedException {
        // Arrange
        int totalQuantity = 5;
        int threadCount = 10;

        Coupon coupon = saveCoupon(totalQuantity);
        Long couponId = coupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            Long memberId = saveMember("user" + i).getId();
            executorService.submit(() -> {
                try {
                    memberCouponService.issueCoupon(memberId, couponId);
                    successCount.incrementAndGet();
                } catch (CoreException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        Coupon result = couponRepository.findById(couponId).orElseThrow();
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(totalQuantity),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - totalQuantity),
            () -> assertThat(result.getIssuedQuantity()).isEqualTo(totalQuantity)
        );
    }

    @Test
    @DisplayName("동일 회원이 같은 쿠폰을 동시에 발급 요청하면 정확히 1개만 발급된다")
    void issueCoupon_concurrently_preventsDuplicateIssuance() throws InterruptedException {
        // Arrange
        int totalQuantity = 100;
        int threadCount = 10;

        Coupon coupon = saveCoupon(totalQuantity);
        Long couponId = coupon.getId();
        Long memberId = saveMember("duplicateUser").getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    memberCouponService.issueCoupon(memberId, couponId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        Coupon result = couponRepository.findById(couponId).orElseThrow();
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(result.getIssuedQuantity()).isEqualTo(1)
        );
    }

    private Coupon saveCoupon(int totalQuantity) {
        Coupon coupon = new Coupon(
            "테스트 쿠폰",
            "동시성 테스트용 쿠폰",
            CouponType.FIXED,
            1000L,
            0L,
            null,
            totalQuantity,
            LocalDateTime.now().minusDays(1),
            LocalDateTime.now().plusDays(30)
        );
        return couponRepository.save(coupon);
    }

    private Member saveMember(String loginId) {
        Member member = new Member(
            loginId,
            passwordEncoder.encode("Password123!"),
            "테스트유저",
            LocalDate.of(1990, 1, 1),
            loginId + "@example.com"
        );
        return memberRepository.save(member);
    }
}
