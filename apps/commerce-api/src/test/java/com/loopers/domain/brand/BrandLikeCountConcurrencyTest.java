package com.loopers.domain.brand;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("브랜드 좋아요 수 동시성 테스트")
class BrandLikeCountConcurrencyTest {

    private static final int MAX_RETRY_COUNT = 10;

    @Autowired
    private BrandService brandService;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동시에 좋아요를 눌러도 낙관적 락 재시도로 정확한 좋아요 수가 유지된다")
    void increaseLikeCount_concurrently_maintainsCorrectCount() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Brand saved = brandRepository.save(new Brand("테스트 브랜드", "설명", null));
        Long brandId = saved.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    retryOnOptimisticLock(() -> brandService.increaseLikeCount(brandId));
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        Brand result = brandService.getBrand(brandId);
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(threadCount),
            () -> assertThat(result.getLikeCount()).isEqualTo(threadCount)
        );
    }

    @Test
    @DisplayName("동시에 좋아요를 취소해도 낙관적 락 재시도로 정확한 좋아요 수가 유지된다")
    void decreaseLikeCount_concurrently_maintainsCorrectCount() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        Brand saved = brandRepository.save(new Brand("테스트 브랜드", "설명", null));
        Long brandId = saved.getId();

        // 초기 좋아요 수를 threadCount로 설정
        for (int i = 0; i < threadCount; i++) {
            brandService.increaseLikeCount(brandId);
        }
        assertThat(brandService.getBrand(brandId).getLikeCount()).isEqualTo(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    retryOnOptimisticLock(() -> brandService.decreaseLikeCount(brandId));
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        Brand result = brandService.getBrand(brandId);
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(threadCount),
            () -> assertThat(result.getLikeCount()).isEqualTo(0L)
        );
    }

    private void retryOnOptimisticLock(Runnable action) {
        int retryCount = 0;
        while (true) {
            try {
                action.run();
                return;
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw e;
                }
            }
        }
    }
}
