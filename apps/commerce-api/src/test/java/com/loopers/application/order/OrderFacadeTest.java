package com.loopers.application.order;

import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressService;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.CouponType;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponService;
import com.loopers.domain.coupon.MemberCouponStatus;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderPeriod;
import com.loopers.domain.order.OrderProduct;
import com.loopers.domain.order.OrderProductStatus;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStatus;
import com.loopers.support.auth.AdminValidator;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock
    private OrderService orderService;

    @Mock
    private MemberService memberService;

    @Mock
    private AddressService addressService;

    @Mock
    private ProductService productService;

    @Mock
    private MemberCouponService memberCouponService;

    @Mock
    private AdminValidator adminValidator;

    @InjectMocks
    private OrderFacade orderFacade;

    private static final String LOGIN_ID = "testuser";
    private static final String PASSWORD = "Password123!";
    private static final Long MEMBER_ID = 1L;
    private static final Long ADDRESS_ID = 1L;
    private static final Long ORDER_ID = 1L;

    @DisplayName("주문 생성")
    @Nested
    class CreateOrder {

        @Test
        @DisplayName("인증 성공 후 주문을 생성한다")
        void createsOrder_afterAuthentication() {
            // arrange
            Member member = createMember();
            Address address = createAddress(ADDRESS_ID, MEMBER_ID);
            Product product = createProduct(1L, "테스트 상품", 10000L);
            ProductOption option = createProductOption(10L, 1L, 1000L, 100);
            Order savedOrder = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);

            OrderCommand.Create command = new OrderCommand.Create(
                ADDRESS_ID,
                "문 앞에 놓아주세요",
                List.of(new OrderCommand.OrderItem(1L, 10L, 2)),
                null
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address));
            given(productService.validateProduct(1L)).willReturn(product);
            given(productService.getProductOption(1L, 10L)).willReturn(option);
            given(orderService.createOrder(any(Order.class))).willReturn(savedOrder);

            // act
            OrderDetailInfo result = orderFacade.createOrder(LOGIN_ID, PASSWORD, command);

            // assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(ORDER_ID),
                () -> verify(productService).decreaseStock(1L, 10L, 2)
            );
        }

        @Test
        @DisplayName("존재하지 않는 배송지로 주문하면 NOT_FOUND 예외가 발생한다")
        void throwsException_whenAddressNotFound() {
            // arrange
            Member member = createMember();
            OrderCommand.Create command = new OrderCommand.Create(
                999L, null, List.of(new OrderCommand.OrderItem(1L, 10L, 1)), null
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of());

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("판매중지 상품으로 주문하면 BAD_REQUEST 예외가 발생한다")
        void throwsException_whenProductIsStopped() {
            // arrange
            Member member = createMember();
            Address address = createAddress(ADDRESS_ID, MEMBER_ID);
            OrderCommand.Create command = new OrderCommand.Create(
                ADDRESS_ID, null, List.of(new OrderCommand.OrderItem(1L, 10L, 1)), null
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address));
            given(productService.validateProduct(1L))
                .willThrow(new CoreException(ErrorType.BAD_REQUEST, "판매중지된 상품입니다."));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("쿠폰을 적용하여 주문을 생성한다")
        void createsOrder_withCouponDiscount() {
            // arrange
            Member member = createMember();
            Address address = createAddress(ADDRESS_ID, MEMBER_ID);
            Product product = createProduct(1L, "테스트 상품", 30000L);
            ProductOption option = createProductOption(10L, 1L, 0L, 100);

            Long memberCouponId = 100L;
            Coupon coupon = new Coupon(1L, "테스트 쿠폰", "설명", CouponType.FIXED, 5000L, 10000L, null,
                1000, 0, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(30),
                null, null, null);
            MemberCoupon memberCoupon = new MemberCoupon(memberCouponId, MEMBER_ID, 1L, "ABCD-1234-EFGH",
                MemberCouponStatus.AVAILABLE, null, null, LocalDateTime.now(), LocalDateTime.now().plusDays(30),
                null, null, 0L, coupon);

            Order savedOrder = new Order(
                ORDER_ID, MEMBER_ID, "ORD20250225-0000001", "테스트 상품",
                "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호", "문 앞에 놓아주세요",
                OrderStatus.PENDING, 30000L, 3000L, 5000L, 28000L
            );

            OrderCommand.Create command = new OrderCommand.Create(
                ADDRESS_ID, "문 앞에 놓아주세요",
                List.of(new OrderCommand.OrderItem(1L, 10L, 1)),
                memberCouponId
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address));
            given(productService.validateProduct(1L)).willReturn(product);
            given(productService.getProductOption(1L, 10L)).willReturn(option);
            given(memberCouponService.validateAndGetCoupon(eq(memberCouponId), eq(MEMBER_ID))).willReturn(memberCoupon);
            given(orderService.createOrder(any(Order.class))).willReturn(savedOrder);

            // act
            OrderDetailInfo result = orderFacade.createOrder(LOGIN_ID, PASSWORD, command);

            // assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(ORDER_ID),
                () -> verify(memberCouponService).validateAndGetCoupon(eq(memberCouponId), eq(MEMBER_ID)),
                () -> verify(memberCouponService).useCoupon(memberCouponId, ORDER_ID)
            );
        }

        @Test
        @DisplayName("타인의 쿠폰으로 주문하면 FORBIDDEN 예외가 발생한다")
        void throwsException_whenCouponNotOwned() {
            // arrange
            Member member = createMember();
            Address address = createAddress(ADDRESS_ID, MEMBER_ID);
            Product product = createProduct(1L, "테스트 상품", 30000L);
            ProductOption option = createProductOption(10L, 1L, 0L, 100);

            Long memberCouponId = 100L;

            OrderCommand.Create command = new OrderCommand.Create(
                ADDRESS_ID, null,
                List.of(new OrderCommand.OrderItem(1L, 10L, 1)),
                memberCouponId
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address));
            given(productService.validateProduct(1L)).willReturn(product);
            given(productService.getProductOption(1L, 10L)).willReturn(option);
            given(memberCouponService.validateAndGetCoupon(eq(memberCouponId), eq(MEMBER_ID)))
                .willThrow(new CoreException(ErrorType.FORBIDDEN, "해당 쿠폰에 대한 권한이 없습니다."));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);
        }

        @Test
        @DisplayName("사용할 수 없는 쿠폰으로 주문하면 BAD_REQUEST 예외가 발생한다")
        void throwsException_whenCouponNotAvailable() {
            // arrange
            Member member = createMember();
            Address address = createAddress(ADDRESS_ID, MEMBER_ID);
            Product product = createProduct(1L, "테스트 상품", 30000L);
            ProductOption option = createProductOption(10L, 1L, 0L, 100);

            Long memberCouponId = 100L;

            OrderCommand.Create command = new OrderCommand.Create(
                ADDRESS_ID, null,
                List.of(new OrderCommand.OrderItem(1L, 10L, 1)),
                memberCouponId
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address));
            given(productService.validateProduct(1L)).willReturn(product);
            given(productService.getProductOption(1L, 10L)).willReturn(option);
            given(memberCouponService.validateAndGetCoupon(eq(memberCouponId), eq(MEMBER_ID)))
                .willThrow(new CoreException(ErrorType.BAD_REQUEST, "사용할 수 없는 쿠폰입니다."));

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("재고가 부족하면 BAD_REQUEST 예외가 발생한다")
        void throwsException_whenInsufficientStock() {
            // arrange
            Member member = createMember();
            Address address = createAddress(ADDRESS_ID, MEMBER_ID);
            Product product = createProduct(1L, "테스트 상품", 10000L);
            ProductOption option = createProductOption(10L, 1L, 1000L, 100);
            OrderCommand.Create command = new OrderCommand.Create(
                ADDRESS_ID, null, List.of(new OrderCommand.OrderItem(1L, 10L, 200)), null
            );

            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(addressService.getAddresses(MEMBER_ID)).willReturn(List.of(address));
            given(productService.validateProduct(1L)).willReturn(product);
            given(productService.getProductOption(1L, 10L)).willReturn(option);
            doThrow(new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다."))
                .when(productService).decreaseStock(eq(1L), eq(10L), anyInt());

            // act & assert
            assertThatThrownBy(() -> orderFacade.createOrder(LOGIN_ID, PASSWORD, command))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("주문 목록 조회")
    @Nested
    class GetOrders {

        @Test
        @DisplayName("인증 성공 후 본인 주문 목록을 반환한다")
        void returnsOrders_afterAuthentication() {
            // arrange
            Member member = createMember();
            Order order1 = createOrder(1L, MEMBER_ID, OrderStatus.PENDING);
            Order order2 = createOrder(2L, MEMBER_ID, OrderStatus.PAID);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrders(eq(MEMBER_ID), any())).willReturn(List.of(order1, order2));

            // act
            List<OrderInfo> result = orderFacade.getOrders(LOGIN_ID, PASSWORD, OrderPeriod.THREE_MONTHS);

            // assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("기간 필터가 적용된다")
        void appliesPeriodFilter() {
            // arrange
            Member member = createMember();
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrders(eq(MEMBER_ID), any())).willReturn(List.of());

            // act
            orderFacade.getOrders(LOGIN_ID, PASSWORD, OrderPeriod.SIX_MONTHS);

            // assert
            verify(orderService).getOrders(eq(MEMBER_ID), any());
        }
    }

    @DisplayName("주문 상세 조회")
    @Nested
    class GetOrderDetail {

        @Test
        @DisplayName("인증 성공 후 본인 주문 상세를 반환한다")
        void returnsOrderDetail_afterAuthentication() {
            // arrange
            Member member = createMember();
            Order order = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);

            // act
            OrderDetailInfo result = orderFacade.getOrderDetail(LOGIN_ID, PASSWORD, ORDER_ID);

            // assert
            assertThat(result.id()).isEqualTo(ORDER_ID);
        }

        @Test
        @DisplayName("타인 주문 조회 시 FORBIDDEN 예외가 발생한다")
        void throwsException_whenNotOwner() {
            // arrange
            Member member = createMember();
            Order order = createOrder(ORDER_ID, 2L, OrderStatus.PENDING);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);
            doThrow(new CoreException(ErrorType.FORBIDDEN, "해당 주문에 대한 권한이 없습니다."))
                .when(orderService).validateOwnership(MEMBER_ID, order);

            // act & assert
            assertThatThrownBy(() -> orderFacade.getOrderDetail(LOGIN_ID, PASSWORD, ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);
        }

        @Test
        @DisplayName("존재하지 않는 주문 시 NOT_FOUND 예외가 발생한다")
        void throwsException_whenOrderNotFound() {
            // arrange
            Member member = createMember();
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrder(999L))
                .willThrow(new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

            // act & assert
            assertThatThrownBy(() -> orderFacade.getOrderDetail(LOGIN_ID, PASSWORD, 999L))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("주문 취소")
    @Nested
    class CancelOrder {

        @Test
        @DisplayName("인증 성공 후 주문을 취소하고 재고를 복구한다")
        void cancelsOrderAndRestoresStock_afterAuthentication() {
            // arrange
            Member member = createMember();
            Order order = createOrderWithProducts(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrderForUpdate(ORDER_ID)).willReturn(order);
            given(orderService.saveOrder(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            OrderDetailInfo result = orderFacade.cancelOrder(LOGIN_ID, PASSWORD, ORDER_ID);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED),
                () -> verify(productService).increaseStock(1L, 10L, 2)
            );
        }

        @Test
        @DisplayName("타인 주문 취소 시 FORBIDDEN 예외가 발생한다")
        void throwsException_whenNotOwner() {
            // arrange
            Member member = createMember();
            Order order = createOrder(ORDER_ID, 2L, OrderStatus.PENDING);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrderForUpdate(ORDER_ID)).willReturn(order);
            doThrow(new CoreException(ErrorType.FORBIDDEN, "해당 주문에 대한 권한이 없습니다."))
                .when(orderService).validateOwnership(MEMBER_ID, order);

            // act & assert
            assertThatThrownBy(() -> orderFacade.cancelOrder(LOGIN_ID, PASSWORD, ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.FORBIDDEN);
        }

        @Test
        @DisplayName("취소 불가 상태 시 BAD_REQUEST 예외가 발생한다")
        void throwsException_whenCannotCancel() {
            // arrange
            Member member = createMember();
            Order order = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PREPARING);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrderForUpdate(ORDER_ID)).willReturn(order);

            // act & assert — order.cancel()에서 직접 예외 발생
            assertThatThrownBy(() -> orderFacade.cancelOrder(LOGIN_ID, PASSWORD, ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
        }

        @Test
        @DisplayName("재고 복구 실패 시 주문 취소도 롤백되어야 한다")
        void rollsBackCancellation_whenStockRestoreFails() {
            // arrange
            Member member = createMember();
            Order order = createOrderWithProducts(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            given(memberService.authenticate(LOGIN_ID, PASSWORD)).willReturn(member);
            given(orderService.getOrderForUpdate(ORDER_ID)).willReturn(order);
            doThrow(new CoreException(ErrorType.INTERNAL_ERROR, "재고 복구 실패"))
                .when(productService).increaseStock(eq(1L), eq(10L), eq(2));

            // act & assert
            assertThatThrownBy(() -> orderFacade.cancelOrder(LOGIN_ID, PASSWORD, ORDER_ID))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INTERNAL_ERROR);

            // 재고 복구 실패 시 saveOrder가 호출되지 않음
            verify(orderService, never()).saveOrder(any(Order.class));
        }
    }

    @DisplayName("Admin 주문 조회")
    @Nested
    class AdminOrders {

        private static final String ADMIN_LDAP = "loopers.admin";

        @Test
        @DisplayName("Admin이 주문 목록을 조회한다")
        void returnsAllOrders_forAdmin() {
            // arrange
            Order order1 = createOrder(1L, MEMBER_ID, OrderStatus.PENDING);
            Order order2 = createOrder(2L, 2L, OrderStatus.PAID);
            doNothing().when(adminValidator).validate(ADMIN_LDAP);
            given(orderService.getOrders(eq(null), any())).willReturn(List.of(order1, order2));

            // act
            List<OrderInfo> result = orderFacade.getOrdersForAdmin(ADMIN_LDAP, OrderPeriod.ALL);

            // assert
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Admin이 주문 상세를 조회한다")
        void returnsOrderDetail_forAdmin() {
            // arrange
            Order order = createOrder(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            doNothing().when(adminValidator).validate(ADMIN_LDAP);
            given(orderService.getOrder(ORDER_ID)).willReturn(order);

            // act
            OrderAdminDetailInfo result = orderFacade.getOrderDetailForAdmin(ADMIN_LDAP, ORDER_ID);

            // assert
            assertAll(
                () -> assertThat(result.id()).isEqualTo(ORDER_ID),
                () -> assertThat(result.memberId()).isEqualTo(MEMBER_ID)
            );
        }

        @Test
        @DisplayName("Admin이 주문 상태를 CANCELLED로 변경하면 재고가 복구된다")
        void restoresStock_whenAdminCancelsOrder() {
            // arrange
            Order order = createOrderWithProducts(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            doNothing().when(adminValidator).validate(ADMIN_LDAP);
            given(orderService.getOrderForUpdate(ORDER_ID)).willReturn(order);
            given(orderService.saveOrder(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

            // act
            OrderAdminDetailInfo result = orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, ORDER_ID, OrderStatus.CANCELLED);

            // assert
            assertAll(
                () -> assertThat(result.status()).isEqualTo(OrderStatus.CANCELLED),
                () -> verify(productService).increaseStock(1L, 10L, 2)
            );
        }

        @Test
        @DisplayName("Admin 주문 취소 시 재고 복구 실패하면 상태 변경도 롤백된다")
        void rollsBackStatusChange_whenStockRestoreFails() {
            // arrange
            Order order = createOrderWithProducts(ORDER_ID, MEMBER_ID, OrderStatus.PENDING);
            doNothing().when(adminValidator).validate(ADMIN_LDAP);
            given(orderService.getOrderForUpdate(ORDER_ID)).willReturn(order);
            doThrow(new CoreException(ErrorType.INTERNAL_ERROR, "재고 복구 실패"))
                .when(productService).increaseStock(eq(1L), eq(10L), eq(2));

            // act & assert
            assertThatThrownBy(() -> orderFacade.changeOrderStatusForAdmin(ADMIN_LDAP, ORDER_ID, OrderStatus.CANCELLED))
                .isInstanceOf(CoreException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.INTERNAL_ERROR);

            // 재고 복구 실패 시 saveOrder가 호출되지 않음
            verify(orderService, never()).saveOrder(any(Order.class));
        }
    }

    private Member createMember() {
        return new Member(MEMBER_ID, LOGIN_ID, "encodedPassword", "테스트", LocalDate.of(1990, 1, 1), "test@example.com");
    }

    private Address createAddress(Long id, Long memberId) {
        return new Address(id, memberId, "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호", true);
    }

    private Product createProduct(Long id, String name, Long price) {
        return new Product(id, name, "PROD001", 1L, 1L, price, ProductStatus.SALE, null, null, 0L, List.of(), List.of(), null, null, null);
    }

    private ProductOption createProductOption(Long id, Long productId, Long extraPrice, int stock) {
        return new ProductOption(id, productId, "옵션1", "옵션1", extraPrice, stock, null, null, null);
    }

    private Order createOrder(Long id, Long memberId, OrderStatus status) {
        return new Order(
            id, memberId, "ORD20250225-0000001", "테스트 주문",
            "홍길동", "010-1234-5678", "06234", "서울시 강남구", "101호", "문 앞에 놓아주세요",
            status, 10000L, 0L, 0L, 10000L
        );
    }

    private Order createOrderWithProducts(Long id, Long memberId, OrderStatus status) {
        Order order = createOrder(id, memberId, status);
        OrderProductStatus productStatus = status == OrderStatus.CANCELLED
            ? OrderProductStatus.CANCELLED
            : OrderProductStatus.NORMAL;
        OrderProduct orderProduct = new OrderProduct(
            1L, 1L, 10L, "테스트 상품", "옵션1", 5000L, 0L, 2, null, productStatus
        );
        order.setOrderProducts(List.of(orderProduct));
        return order;
    }
}
