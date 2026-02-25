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
class OrderV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberRepository memberRepository;
    private final AddressRepository addressRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public OrderV1ApiE2ETest(
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

    @DisplayName("POST /api/v1/orders - 주문 생성")
    @Nested
    class CreateOrder {

        private Member member;
        private Address address;
        private Product product;
        private ProductOption option;

        @BeforeEach
        void setUp() {
            member = saveMember("user1", "Password123!");
            address = saveAddress(member.getId());
            Brand brand = saveBrand("Nike");
            Category category = saveCategory("의류");
            option = new ProductOption(null, "M", "M 사이즈", 1000L, 100);
            product = saveProductWithOption("테스트 상품", brand.getId(), category.getId(), 10000L, option);
        }

        @Test
        @DisplayName("주문 생성 시 201 Created를 반환한다")
        void returnsCreated_whenCreateOrder() {
            // arrange
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(),
                "문 앞에 놓아주세요",
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 2))
            );

            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = createOrder(
                member.getLoginId(), "Password123!", request
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.PENDING),
                () -> assertThat(response.getBody().data().orderProducts()).hasSize(1)
            );
        }

        @Test
        @DisplayName("인증 실패 시 401 Unauthorized를 반환한다")
        void returnsUnauthorized_whenAuthenticationFails() {
            // arrange
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1))
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = createOrderWithError(
                member.getLoginId(), "WrongPassword!", request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("존재하지 않는 배송지로 주문하면 404 Not Found를 반환한다")
        void returnsNotFound_whenAddressNotExists() {
            // arrange
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                999L, null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1))
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = createOrderWithError(
                member.getLoginId(), "Password123!", request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("재고 부족 시 400 Bad Request를 반환한다")
        void returnsBadRequest_whenInsufficientStock() {
            // arrange
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 200))
            );

            // act
            ResponseEntity<ApiResponse<Object>> response = createOrderWithError(
                member.getLoginId(), "Password123!", request
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }

        private ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> createOrder(
            String loginId, String password, OrderV1Dto.CreateOrderRequest request
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            headers.setContentType(MediaType.APPLICATION_JSON);
            return testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> createOrderWithError(
            String loginId, String password, OrderV1Dto.CreateOrderRequest request
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            headers.setContentType(MediaType.APPLICATION_JSON);
            return testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("GET /api/v1/orders - 주문 목록 조회")
    @Nested
    class GetOrders {

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
        }

        @Test
        @DisplayName("주문 목록을 조회하면 200 OK를 반환한다")
        void returnsOk_whenGetOrders() {
            // arrange
            createOrderForTest();

            // act
            ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> response = getOrders(
                member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).hasSize(1)
            );
        }

        @Test
        @DisplayName("주문이 없으면 빈 목록을 반환한다")
        void returnsEmptyList_whenNoOrders() {
            // act
            ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> response = getOrders(
                member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).isEmpty()
            );
        }

        private void createOrderForTest() {
            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1))
            );
            testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<ApiResponse<OrderV1Dto.OrderDetailResponse>>() {}
            );
        }

        private ResponseEntity<ApiResponse<List<OrderV1Dto.OrderResponse>>> getOrders(String loginId, String password) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("GET /api/v1/orders/{orderId} - 주문 상세 조회")
    @Nested
    class GetOrderDetail {

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
        @DisplayName("본인 주문 조회 시 200 OK를 반환한다")
        void returnsOk_whenGetOwnOrder() {
            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = getOrderDetail(
                orderId, member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().id()).isEqualTo(orderId)
            );
        }

        @Test
        @DisplayName("타인 주문 조회 시 403 Forbidden을 반환한다")
        void returnsForbidden_whenNotOwner() {
            // arrange
            Member otherMember = saveMember("user2", "Password123!");

            // act
            ResponseEntity<ApiResponse<Object>> response = getOrderDetailWithError(
                orderId, otherMember.getLoginId(), "Password123!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 주문 조회 시 404 Not Found를 반환한다")
        void returnsNotFound_whenOrderNotExists() {
            // act
            ResponseEntity<ApiResponse<Object>> response = getOrderDetailWithError(
                999L, member.getLoginId(), "Password123!"
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        private Long createOrderAndGetId() {
            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1))
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody().data().id();
        }

        private ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> getOrderDetail(
            Long orderId, String loginId, String password
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }

        private ResponseEntity<ApiResponse<Object>> getOrderDetailWithError(
            Long orderId, String loginId, String password
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/orders/" + orderId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    @DisplayName("PATCH /api/v1/orders/{orderId}/cancel - 주문 취소")
    @Nested
    class CancelOrder {

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
        @DisplayName("PENDING 상태 취소 시 200 OK를 반환한다")
        void returnsOk_whenCancelPendingOrder() {
            // act
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = cancelOrder(
                orderId, member.getLoginId(), "Password123!"
            );

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data().status()).isEqualTo(OrderStatus.CANCELLED)
            );
        }

        @Test
        @DisplayName("취소 후 재고가 복구된다")
        void restoresStock_afterCancel() {
            // arrange
            // Note: @BeforeEach already created an order with quantity=1, so stock is 99 at this point
            Product currentProduct = productRepository.findById(product.getId()).orElseThrow();
            int stockBeforeNewOrder = currentProduct.getOptions().get(0).getStockQuantity();
            int orderedQuantity = 5;

            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), orderedQuantity))
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> createResponse = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
            Long newOrderId = createResponse.getBody().data().id();

            // act
            cancelOrder(newOrderId, member.getLoginId(), "Password123!");

            // assert
            Product updatedProduct = productRepository.findById(product.getId()).orElseThrow();
            int finalStock = updatedProduct.getOptions().stream()
                .filter(o -> o.getId().equals(product.getOptions().get(0).getId()))
                .findFirst()
                .map(ProductOption::getStockQuantity)
                .orElse(0);
            assertThat(finalStock).isEqualTo(stockBeforeNewOrder);
        }

        private Long createOrderAndGetId() {
            HttpHeaders headers = createAuthHeaders(member.getLoginId(), "Password123!");
            headers.setContentType(MediaType.APPLICATION_JSON);
            OrderV1Dto.CreateOrderRequest request = new OrderV1Dto.CreateOrderRequest(
                address.getId(), null,
                List.of(new OrderV1Dto.OrderItemRequest(product.getId(), product.getOptions().get(0).getId(), 1))
            );
            ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> response = testRestTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                new ParameterizedTypeReference<>() {}
            );
            return response.getBody().data().id();
        }

        private ResponseEntity<ApiResponse<OrderV1Dto.OrderDetailResponse>> cancelOrder(
            Long orderId, String loginId, String password
        ) {
            HttpHeaders headers = createAuthHeaders(loginId, password);
            return testRestTemplate.exchange(
                "/api/v1/orders/" + orderId + "/cancel",
                HttpMethod.PATCH,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {}
            );
        }
    }

    private HttpHeaders createAuthHeaders(String loginId, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Loopers-LoginId", loginId);
        headers.set("X-Loopers-LoginPw", password);
        return headers;
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
