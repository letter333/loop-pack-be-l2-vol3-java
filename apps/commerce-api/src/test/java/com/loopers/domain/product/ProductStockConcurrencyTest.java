package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("상품 재고 동시성 테스트")
class ProductStockConcurrencyTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동시에 재고를 차감해도 정확한 재고가 유지된다")
    void decreaseStock_concurrently_maintainsCorrectStock() throws InterruptedException {
        // Arrange
        int initialStock = 100;
        int threadCount = 10;
        int quantityPerThread = 1;

        List<ProductOption> options = List.of(
            new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, initialStock)
        );
        Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
        Long productId = saved.getId();
        Long optionId = saved.getOptions().get(0).getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.decreaseStock(productId, optionId, quantityPerThread);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executorService.shutdown();

        // Assert
        Product result = productService.getProduct(productId);
        int expectedStock = initialStock - (threadCount * quantityPerThread);
        assertThat(result.getOption(optionId).getStockQuantity()).isEqualTo(expectedStock);
    }

    @Test
    @DisplayName("재고보다 많은 동시 요청 시 정확히 재고 수만큼만 성공한다")
    void decreaseStock_concurrently_preventsOverselling() throws InterruptedException {
        // Arrange
        int initialStock = 5;
        int threadCount = 10;
        int quantityPerThread = 1;

        List<ProductOption> options = List.of(
            new ProductOption(null, "BLACK_M", "블랙 / M", 5000L, initialStock)
        );
        Product saved = productRepository.save(new Product("아이폰 15", 1L, 1L, 1500000L, options, null));
        Long productId = saved.getId();
        Long optionId = saved.getOptions().get(0).getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    productService.decreaseStock(productId, optionId, quantityPerThread);
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
        Product result = productService.getProduct(productId);
        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(initialStock),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - initialStock),
            () -> assertThat(result.getOption(optionId).getStockQuantity()).isEqualTo(0)
        );
    }
}
