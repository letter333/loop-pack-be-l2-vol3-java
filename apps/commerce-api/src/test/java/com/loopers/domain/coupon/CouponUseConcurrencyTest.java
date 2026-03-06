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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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
@DisplayName("쿠폰 사용 동시성 테스트")
class CouponUseConcurrencyTest {

    @Autowired
    private MemberCouponService memberCouponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

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
    @DisplayName("동시에 같은 쿠폰을 사용해도 정확히 1번만 사용된다")
    void useCoupon_concurrently_onlyOneSucceeds() throws InterruptedException {
        // Arrange
        int threadCount = 10;

        Member member = saveMember("user1");
        Coupon coupon = saveCoupon(100);
        MemberCoupon memberCoupon = memberCouponService.issueCoupon(member.getId(), coupon.getId());
        Long memberCouponId = memberCoupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            long orderId = i + 1L;
            executorService.submit(() -> {
                try {
                    memberCouponService.useCoupon(memberCouponId, orderId);
                    successCount.incrementAndGet();
                } catch (CoreException | ObjectOptimisticLockingFailureException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        MemberCoupon result = memberCouponRepository.findById(memberCouponId).orElseThrow();
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(result.getStatus()).isEqualTo(MemberCouponStatus.USED)
        );
    }

    @Test
    @DisplayName("동시 사용 시 1개만 성공하고 나머지는 예외가 발생한다")
    void useCoupon_concurrently_failsWithCorrectException() throws InterruptedException {
        // Arrange
        int threadCount = 5;

        Member member = saveMember("user2");
        Coupon coupon = saveCoupon(100);
        MemberCoupon memberCoupon = memberCouponService.issueCoupon(member.getId(), coupon.getId());
        Long memberCouponId = memberCoupon.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger badRequestCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            long orderId = i + 100L;
            executorService.submit(() -> {
                try {
                    memberCouponService.useCoupon(memberCouponId, orderId);
                    successCount.incrementAndGet();
                } catch (CoreException | ObjectOptimisticLockingFailureException e) {
                    badRequestCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        MemberCoupon result = memberCouponRepository.findById(memberCouponId).orElseThrow();
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(badRequestCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(result.getStatus()).isEqualTo(MemberCouponStatus.USED),
            () -> assertThat(result.getUsedOrderId()).isNotNull()
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
