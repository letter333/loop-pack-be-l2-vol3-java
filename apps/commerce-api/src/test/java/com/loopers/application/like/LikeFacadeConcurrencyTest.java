package com.loopers.application.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("LikeFacade 상품 좋아요 동시성 통합 테스트")
class LikeFacadeConcurrencyTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String RAW_PASSWORD = "Password123!";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("N명의 서로 다른 유저가 동시에 같은 상품에 좋아요를 누르면 like_count == N이 된다")
    void concurrentLike_byDifferentUsers_maintainsCorrectLikeCount() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            members.add(saveMember("user" + i, RAW_PASSWORD));
        }
        Brand brand = brandRepository.save(new Brand("테스트 브랜드", "설명", null));
        Product product = productRepository.save(new Product("테스트 상품", brand.getId(), 1L, 10000L));
        Long productId = product.getId();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act
        for (int i = 0; i < threadCount; i++) {
            Member member = members.get(i);
            executorService.submit(() -> {
                try {
                    likeFacade.toggleProductLike(member.getLoginId(), RAW_PASSWORD, productId);
                    successCount.incrementAndGet();
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
            () -> assertThat(successCount.get()).isEqualTo(threadCount),
            () -> assertThat(result.getLikeCount()).isEqualTo((long) threadCount)
        );
    }

    @Test
    @DisplayName("N명이 좋아요한 후 동시에 취소하면 like_count == 0이 된다")
    void concurrentUnlike_byDifferentUsers_maintainsCorrectLikeCount() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            members.add(saveMember("user" + i, RAW_PASSWORD));
        }
        Brand brand = brandRepository.save(new Brand("테스트 브랜드", "설명", null));
        Product product = productRepository.save(new Product("테스트 상품", brand.getId(), 1L, 10000L));
        Long productId = product.getId();

        // 순차적으로 좋아요 생성
        for (Member member : members) {
            likeFacade.toggleProductLike(member.getLoginId(), RAW_PASSWORD, productId);
        }
        assertThat(productService.getProduct(productId).getLikeCount()).isEqualTo(threadCount);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // Act - 동시에 좋아요 취소
        for (int i = 0; i < threadCount; i++) {
            Member member = members.get(i);
            executorService.submit(() -> {
                try {
                    likeFacade.toggleProductLike(member.getLoginId(), RAW_PASSWORD, productId);
                    successCount.incrementAndGet();
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
            () -> assertThat(successCount.get()).isEqualTo(threadCount),
            () -> assertThat(result.getLikeCount()).isEqualTo(0L)
        );
    }

    private Member saveMember(String loginId, String rawPassword) {
        Member member = new Member(loginId, rawPassword, "Test User",
            LocalDate.of(1990, 1, 1), loginId + "@example.com");
        member.encryptPassword(passwordEncoder.encode(rawPassword));
        return memberRepository.save(member);
    }
}
