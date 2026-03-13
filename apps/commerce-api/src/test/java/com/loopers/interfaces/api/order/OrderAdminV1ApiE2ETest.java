package com.loopers.interfaces.api.order;

import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressRepository;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryRepository;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberRepository;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderAdminV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseCleanUp databaseCleanUp;

    private static final String ADMIN_LDAP = "loopers.admin";

    @Autowired
    public OrderAdminV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        MemberRepository memberRepository,
        AddressRepository addressRepository,
        ProductRepository productRepository,
        BrandRepository brandRepository,
        CategoryRepository categoryRepository,
        PasswordEncoder passwordEncoder,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberRepository = memberRepository;
        this.addressRepository = addressRepository;
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.passwordEncoder = passwordEncoder;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/admin/orders - Admin 주문 목록 조회")
    @Nested
    class GetOrdersForAdmin {

        private Member member;
        private Address address;
        private Product product;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            address = saveAddress(member.getId());
            Brand brand = saveBrand("Nike");
            Category category = saveCategory("의류");
            ProductOption option = new ProductOption(null, "M", "M 사이즈", 1000L, 100);
            product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 10000L, option);
            createOrderForTest();
        }

        @Test
        @DisplayName("관리자 목록 조회 시 200 OK를 반환한다")
        void returnsOk_whenAdmin() {
            // act
            ResponseEntity<ApiResponse<List<OrderAdminV1Dto.OrderAdminResponse>>> response = getOrdersForAdmin(ADMIN_LDAP);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(1)
            );
        }

        @Test
        @DisplayName("비관리자 조회 시 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotAdmin() {
            // act
            ResponseEntity<ApiResponse<Object>> response = getOrdersForAdminWithError("invalid.ldap");

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        private void createOrderForTest() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", member.getLoginId());
            headers.set("X-Loopers-LoginPw", "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1)),
                null
            );
            testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderDetailResponse>>() {}
            );
        }

        private ResponseEntity<ApiResponse<List<OrderAdminV1Dto.OrderAdminResponse>>> getOrdersForAdmin(String ldap) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-Ldap", ldap);
            return testRestTemplate.exchange(
                "/api/v1/admin/orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> getOrdersForAdminWithError(String ldap) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-Ldap", ldap);
            return testRestTemplate.exchange(
                "/api/v1/admin/orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("GET /api/v1/admin/orders/{orderId} - Admin 주문 상세 조회")
    @Nested
    class GetOrderDetailForAdmin {

        private Member member;
        private Address address;
        private Product product;
        private Long orderId;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            address = saveAddress(member.getId());
            Brand brand = saveBrand("Nike");
            Category category = saveCategory("의류");
            ProductOption option = new ProductOption(null, "M", "M 사이즈", 1000L, 100);
            product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 10000L, option);
            orderId = createOrderAndGetId();
        }

        @Test
        @DisplayName("관리자 상세 조회 시 200 OK를 반환한다")
        void returnsOk_whenAdmin() {
            // act
            ResponseEntity<ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse>> response = getOrderDetailForAdmin(
                ADMIN_LDAP, orderId
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(orderId),
                () -> assertThat(response.getBody().data().memberId()).isEqualTo(member.getId())
            );
        }

        @Test
        @DisplayName("비관리자 조회 시 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotAdmin() {
            // act
            ResponseEntity<ApiResponse<Object>> response = getOrderDetailForAdminWithError("invalid.ldap", orderId);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        private Long createOrderAndGetId() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", member.getLoginId());
            headers.set("X-Loopers-LoginPw", "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1)),
                null
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody().data().id();
        }

        private ResponseEntity<ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse>> getOrderDetailForAdmin(
            String ldap, Long orderId
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-Ldap", ldap);
            return testRestTemplate.exchange(
                "/api/v1/admin/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> getOrderDetailForAdminWithError(String ldap, Long orderId) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-Ldap", ldap);
            return testRestTemplate.exchange(
                "/api/v1/admin/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("PATCH /api/v1/admin/orders/{orderId}/status - Admin 주문 상태 변경")
    @Nested
    class ChangeOrderStatusForAdmin {

        private Member member;
        private Address address;
        private Product product;
        private Long orderId;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            address = saveAddress(member.getId());
            Brand brand = saveBrand("Nike");
            Category category = saveCategory("의류");
            ProductOption option = new ProductOption(null, "M", "M 사이즈", 1000L, 100);
            product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 10000L, option);
            orderId = createOrderAndGetId();
        }

        @Test
        @DisplayName("관리자 상태 변경 성공 시 200 OK를 반환한다")
        void returnsOk_whenAdminChangesStatus() {
            // arrange
            payOrder(orderId);
            OrderAdminV1Dto.ChangeStatusRequest request = new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.PREPARING);

            // act
            ResponseEntity<ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse>> response = changeOrderStatus(
                ADMIN_LDAP, orderId, request
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(orderId),
                () -> assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.PREPARING)
            );
        }

        @Test
        @DisplayName("비관리자 요청 시 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotAdmin() {
            // arrange
            OrderAdminV1Dto.ChangeStatusRequest request = new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.PAID);

            // act
            ResponseEntity<ApiResponse<Object>> response = changeOrderStatusWithError(
                "invalid.ldap", orderId, request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("잘못된 상태 전환 요청 시 400 Bad Request를 반환한다")
        void returnsBadRequest_whenInvalidStatusTransition() {
            // arrange - PENDING 상태에서 바로 SHIPPING으로 변경 시도
            OrderAdminV1Dto.ChangeStatusRequest request = new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.SHIPPING);

            // act
            ResponseEntity<ApiResponse<Object>> response = changeOrderStatusWithError(
                ADMIN_LDAP, orderId, request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("CANCELLED로 상태 변경 시 재고가 복구된다")
        void restoresStock_whenStatusChangedToCancelled() {
            // arrange
            int initialStock = product.getOptions().get(0).getStockQuantity();
            int orderedQuantity = 1;
            int stockAfterOrder = initialStock - orderedQuantity;

            // 주문 후 재고 확인
            Product productAfterOrder = productRepository.findById(product.getId()).orElseThrow();
            assertThat(productAfterOrder.getOptions().get(0).getStockQuantity()).isEqualTo(stockAfterOrder);

            OrderAdminV1Dto.ChangeStatusRequest request = new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.CANCELLED);

            // act
            ResponseEntity<ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse>> response = changeOrderStatus(
                ADMIN_LDAP, orderId, request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.CANCELLED);

            // 재고 복구 확인
            Product productAfterCancel = productRepository.findById(product.getId()).orElseThrow();
            assertThat(productAfterCancel.getOptions().get(0).getStockQuantity()).isEqualTo(initialStock);
        }

        @Test
        @DisplayName("RETURNED로 상태 변경 시 재고는 복구되지 않는다")
        void doesNotRestoreStock_whenStatusChangedToReturned() {
            // arrange - 주문 진행 (PENDING -> PAID -> PREPARING -> SHIPPING -> DELIVERED -> RETURNED)
            payOrder(orderId);
            changeOrderStatus(ADMIN_LDAP, orderId, new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.PREPARING));
            changeOrderStatus(ADMIN_LDAP, orderId, new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.SHIPPING));
            changeOrderStatus(ADMIN_LDAP, orderId, new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.DELIVERED));

            int stockBeforeReturn = productRepository.findById(product.getId()).orElseThrow()
                .getOptions().get(0).getStockQuantity();

            OrderAdminV1Dto.ChangeStatusRequest request = new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.RETURNED);

            // act
            ResponseEntity<ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse>> response = changeOrderStatus(
                ADMIN_LDAP, orderId, request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.RETURNED);

            // 재고 변화 없음 확인
            Product productAfterReturn = productRepository.findById(product.getId()).orElseThrow();
            assertThat(productAfterReturn.getOptions().get(0).getStockQuantity()).isEqualTo(stockBeforeReturn);
        }

        private Long createOrderAndGetId() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-LoginId", member.getLoginId());
            headers.set("X-Loopers-LoginPw", "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1)),
                null
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody().data().id();
        }

        private void payOrder(Long orderId) {
            // PENDING -> PAID 상태 변경 (실제로는 결제 로직이 있지만, 테스트에서는 Admin API로 변경)
            changeOrderStatus(ADMIN_LDAP, orderId, new OrderAdminV1Dto.ChangeStatusRequest(OrderStatus.PAID));
        }

        private ResponseEntity<ApiResponse<OrderAdminV1Dto.OrderAdminDetailResponse>> changeOrderStatus(
            String ldap, Long orderId, OrderAdminV1Dto.ChangeStatusRequest request
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-Ldap", ldap);
            headers.setContentType(MediaType.APPLICATION_JSON);
            return testRestTemplate.exchange(
                "/api/v1/admin/orders/" + orderId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> changeOrderStatusWithError(
            String ldap, Long orderId, OrderAdminV1Dto.ChangeStatusRequest request
        ) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Loopers-Ldap", ldap);
            headers.setContentType(MediaType.APPLICATION_JSON);
            return testRestTemplate.exchange(
                "/api/v1/admin/orders/" + orderId + "/status",
                HttpMethod.PATCH,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    private Member saveMember(String loginId, String rawPassword) {
        Member member = new Member(loginId, rawPassword, "Test User",
            LocalDate.of(1990, 1, 1), loginId + "@example.com");
        member.encryptPassword(passwordEncoder.encode(rawPassword));
        return memberRepository.save(member);
    }

    private Address saveAddress(Long memberId) {
        Address address = new Address(memberId, "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호");
        return addressRepository.save(address);
    }

    private Brand saveBrand(String name) {
        Brand brand = new Brand(name, "Description", "https://example.com/logo.png");
        return brandRepository.save(brand);
    }

    private Category saveCategory(String name) {
        Category category = new Category(name);
        return categoryRepository.save(category);
    }

    private Product saveProductWithOption(String name, Long brandId, Long categoryId, Long basePrice, ProductOption option) {
        Product product = new Product(name, brandId, categoryId, basePrice, List.of(option), List.of());
        return productRepository.save(product);
    }
}
