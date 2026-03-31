package com.loopers.application.order;

import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressRepository;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryRepository;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.queue.QueueTokenService;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@DisplayName("주문 상태 변경 동시성 테스트")
class OrderStatusChangeConcurrencyTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private QueueTokenService queueTokenService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Password123!";
    private static final String ADMIN_LDAP = "loopers.admin";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @Test
    @DisplayName("10개 스레드가 동시에 같은 주문(PAID)을 PREPARING으로 변경해도 정확히 1번만 성공한다")
    void changeStatus_concurrently_onlyOneSucceeds() throws InterruptedException {
        // Arrange
        int threadCount = 10;

        Member member = saveMember(LOGIN_ID, PASSWORD);
        Address address = saveAddress(member.getId());
        Brand brand = saveBrand();
        Category category = saveCategory();
        ProductOption option = new ProductOption(null, "기본", "기본", 0L, 100);
        Product product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 10000L, option);

        Long productOptionId = product.getOptions().get(0).getId();

        // 주문 생성
        OrderCommand.Create createCommand = new OrderCommand.Create(
            address.getId(),
            "문 앞에 놓아주세요",
            List.of(new OrderCommand.OrderItem(product.getId(), productOptionId, 1)),
            null
        );
        OrderDetailInfo createdOrder = orderFacade.createOrder(LOGIN_ID, PASSWORD, queueTokenService.issueToken("order", member.getId()), createCommand);
        Long orderId = createdOrder.id();

        // PENDING → PAID 상태로 변경
        orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, orderId, OrderStatus.PAID);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: 10개 스레드가 동시에 PAID → PREPARING 상태 변경
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, orderId, OrderStatus.PREPARING);
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
        Order updatedOrder = orderRepository.findById(orderId).orElseThrow();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PREPARING)
        );
    }

    @Test
    @DisplayName("상태 변경(PREPARING)과 취소(CANCELLED)가 동시에 실행되면 정확히 1번만 성공하고, 취소 시 재고가 정확히 복구된다")
    void changeStatusAndCancel_concurrently_onlyOneSucceeds() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        int orderQuantity = 3;
        int initialStock = 10;

        Member member = saveMember(LOGIN_ID, PASSWORD);
        Address address = saveAddress(member.getId());
        Brand brand = saveBrand();
        Category category = saveCategory();
        ProductOption option = new ProductOption(null, "기본", "기본", 0L, initialStock);
        Product product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 10000L, option);

        Long productOptionId = product.getOptions().get(0).getId();

        // 주문 생성 (재고 차감됨)
        OrderCommand.Create createCommand = new OrderCommand.Create(
            address.getId(),
            "문 앞에 놓아주세요",
            List.of(new OrderCommand.OrderItem(product.getId(), productOptionId, orderQuantity)),
            null
        );
        OrderDetailInfo createdOrder = orderFacade.createOrder(LOGIN_ID, PASSWORD, queueTokenService.issueToken("order", member.getId()), createCommand);
        Long orderId = createdOrder.id();

        // 재고 차감 확인
        Product afterOrder = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterOrder.getOptions().get(0).getStockQuantity()).isEqualTo(initialStock - orderQuantity);

        // PENDING → PAID 상태로 변경
        orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, orderId, OrderStatus.PAID);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: 상태 변경(PREPARING) 5개 + 취소(CANCELLED) 5개 동시 실행
        for (int i = 0; i < threadCount; i++) {
            final boolean isPreparing = i % 2 == 0;
            executorService.submit(() -> {
                try {
                    if (isPreparing) {
                        orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, orderId, OrderStatus.PREPARING);
                    } else {
                        orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, orderId, OrderStatus.CANCELLED);
                    }
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
        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        Product afterConcurrency = productRepository.findById(product.getId()).orElseThrow();
        int finalStock = afterConcurrency.getOptions().get(0).getStockQuantity();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - 1),
            () -> {
                if (finalOrder.getStatus() == OrderStatus.CANCELLED) {
                    // 취소가 성공했다면 재고가 복구되어야 한다
                    assertThat(finalStock).isEqualTo(initialStock);
                } else {
                    // PREPARING이 성공했다면 재고는 차감 상태 유지
                    assertThat(finalStock).isEqualTo(initialStock - orderQuantity);
                }
            }
        );
    }

    private Member saveMember(String loginId, String rawPassword) {
        Member member = new Member(loginId, rawPassword, "테스트유저",
            LocalDate.of(1990, 1, 1), loginId + "@example.com");
        member.encryptPassword(passwordEncoder.encode(rawPassword));
        return memberRepository.save(member);
    }

    private Address saveAddress(Long memberId) {
        Address address = new Address(memberId, "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호");
        return addressRepository.save(address);
    }

    private Brand saveBrand() {
        Brand brand = new Brand("테스트 브랜드", "설명", "https://example.com/logo.png");
        return brandRepository.save(brand);
    }

    private Category saveCategory() {
        Category category = new Category("테스트 카테고리");
        return categoryRepository.save(category);
    }

    private Product saveProductWithOption(String name, Long brandId, Long categoryId, Long basePrice, ProductOption option) {
        Product product = new Product(name, brandId, categoryId, basePrice, List.of(option), List.of());
        return productRepository.save(product);
    }
}
