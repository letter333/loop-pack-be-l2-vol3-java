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
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
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
@DisplayName("주문 취소 동시성 테스트")
class OrderCancelConcurrencyTest {

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
    private DatabaseCleanUp databaseCleanUp;

    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Password123!";
    private static final String ADMIN_LDAP = "loopers.admin";

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("동시에 같은 주문을 취소해도 재고는 정확히 1번만 복구된다")
    void cancelOrder_concurrently_restoresStockOnlyOnce() throws InterruptedException {
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
        OrderDetailInfo createdOrder = orderFacade.createOrder(LOGIN_ID, PASSWORD, createCommand);
        Long orderId = createdOrder.id();

        // 재고 차감 확인 (initialStock - orderQuantity)
        Product afterOrder = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterOrder.getOptions().get(0).getStockQuantity()).isEqualTo(initialStock - orderQuantity);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: 10개 스레드가 동시에 같은 주문 취소
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    orderFacade.cancelOrder(LOGIN_ID, PASSWORD, orderId);
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
        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        Product afterCancel = productRepository.findById(product.getId()).orElseThrow();
        int restoredStock = afterCancel.getOptions().get(0).getStockQuantity();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED),
            () -> assertThat(restoredStock).isEqualTo(initialStock)
        );
    }

    @Test
    @DisplayName("사용자 취소와 관리자 취소가 동시에 들어와도 재고는 정확히 1번만 복구된다")
    void cancelOrder_concurrently_withAdmin_restoresStockOnlyOnce() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        int orderQuantity = 2;
        int initialStock = 10;

        Member member = saveMember(LOGIN_ID, PASSWORD);
        Address address = saveAddress(member.getId());
        Brand brand = saveBrand();
        Category category = saveCategory();
        ProductOption option = new ProductOption(null, "기본", "기본", 0L, initialStock);
        Product product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 20000L, option);

        Long productOptionId = product.getOptions().get(0).getId();

        // 주문 생성
        OrderCommand.Create createCommand = new OrderCommand.Create(
            address.getId(),
            "부재 시 경비실",
            List.of(new OrderCommand.OrderItem(product.getId(), productOptionId, orderQuantity)),
            null
        );
        OrderDetailInfo createdOrder = orderFacade.createOrder(LOGIN_ID, PASSWORD, createCommand);
        Long orderId = createdOrder.id();

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // Act: 사용자 취소 5 + 관리자 취소 5 동시 실행
        for (int i = 0; i < threadCount; i++) {
            final boolean isUserCancel = i % 2 == 0;
            executorService.submit(() -> {
                try {
                    if (isUserCancel) {
                        orderFacade.cancelOrder(LOGIN_ID, PASSWORD, orderId);
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
        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        Product afterCancel = productRepository.findById(product.getId()).orElseThrow();
        int restoredStock = afterCancel.getOptions().get(0).getStockQuantity();

        assertAll(
            () -> assertThat(successCount.get()).isEqualTo(1),
            () -> assertThat(failCount.get()).isEqualTo(threadCount - 1),
            () -> assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED),
            () -> assertThat(restoredStock).isEqualTo(initialStock)
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
