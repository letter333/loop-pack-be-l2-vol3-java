package com.loopers.application.order;

import com.loopers.application.event.OrderCompletedEvent;
import com.loopers.application.product.ProductCacheEvictEvent;
import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressService;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.MemberCouponService;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderPeriod;
import com.loopers.domain.order.OrderProduct;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.product.ImageType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductImage;
import com.loopers.domain.product.ProductOption;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.queue.QueueService;
import com.loopers.domain.queue.QueueTokenService;
import com.loopers.support.auth.AdminValidator;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final MemberService memberService;
    private final AddressService addressService;
    private final ProductService productService;
    private final MemberCouponService memberCouponService;
    private final QueueService queueService;
    private final QueueTokenService queueTokenService;
    private final AdminValidator adminValidator;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String ORDER_EVENT_TYPE = "order";

    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class,
        maxAttempts = 3,
        backoff = @Backoff(delay = 50, multiplier = 2)
    )
    @Transactional
    public OrderDetailInfo createOrder(String loginId, String password, String queueToken, OrderCommand.Create command) {
        Member member = memberService.authenticate(loginId, password);

        boolean queueActive = queueService.isQueueActive(ORDER_EVENT_TYPE);
        if (queueActive) {
            if (queueToken == null || queueToken.isBlank()) {
                throw new CoreException(ErrorType.FORBIDDEN, "대기열 진입이 필요합니다.");
            }
            long tokenTtl = queueTokenService.validateAndConsume(ORDER_EVENT_TYPE, member.getId(), queueToken);
            try {
                return executeCreateOrder(member, command);
            } catch (Exception e) {
                queueTokenService.restoreToken(ORDER_EVENT_TYPE, member.getId(), queueToken, tokenTtl);
                throw e;
            }
        }

        return executeCreateOrder(member, command);
    }

    private OrderDetailInfo executeCreateOrder(Member member, OrderCommand.Create command) {
        Address address = findAddressForMember(member.getId(), command.addressId());

        Order order = new Order(
            member.getId(),
            address.getRecipientName(),
            address.getPhone(),
            address.getZipCode(),
            address.getAddress(),
            address.getAddressDetail(),
            command.shippingMemo()
        );

        // productId 오름차순 정렬 → 락 획득 순서 고정으로 데드락 방지
        List<OrderCommand.OrderItem> sortedItems = command.items().stream()
            .sorted(Comparator.comparing(OrderCommand.OrderItem::productId))
            .toList();

        for (OrderCommand.OrderItem item : sortedItems) {
            Product product = productService.validateProduct(item.productId());
            ProductOption option = productService.getProductOption(item.productId(), item.productOptionId());

            String thumbnailUrl = getThumbnailUrl(product);

            OrderProduct orderProduct = new OrderProduct(
                item.productId(),
                item.productOptionId(),
                product.getName(),
                option.getDisplayName(),
                product.getBasePrice(),
                option.getExtraPrice() != null ? option.getExtraPrice() : 0L,
                item.quantity(),
                thumbnailUrl
            );
            order.addOrderProduct(orderProduct);

            productService.decreaseStock(item.productId(), item.productOptionId(), item.quantity());
        }
        Set<Long> productIds = sortedItems.stream()
            .map(OrderCommand.OrderItem::productId)
            .collect(Collectors.toSet());
        applicationEventPublisher.publishEvent(ProductCacheEvictEvent.of(productIds));

        // 쿠폰 적용
        MemberCoupon memberCoupon = null;
        if (command.memberCouponId() != null) {
            memberCoupon = memberCouponService.validateAndGetCoupon(
                command.memberCouponId(), member.getId());
            Long discountAmount = memberCoupon.getCoupon().calculateDiscount(order.getTotalAmount());
            order.applyCouponDiscount(discountAmount);
        }

        Order savedOrder = orderService.createOrder(order);

        // 주문 저장 후 쿠폰 사용 처리
        if (memberCoupon != null) {
            memberCouponService.useCoupon(command.memberCouponId(), savedOrder.getId());
        }

        // 부가 로직: 주문 완료 이벤트 발행 (유저 행동 로깅용)
        applicationEventPublisher.publishEvent(new OrderCompletedEvent(
            savedOrder.getId(), member.getId(), productIds, savedOrder.getPaymentAmount()
        ));

        return OrderDetailInfo.from(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getOrders(String loginId, String password, OrderPeriod period) {
        Member member = memberService.authenticate(loginId, password);
        LocalDateTime startDate = period != null ? period.getStartDate() : null;
        return orderService.getOrders(member.getId(), startDate)
            .stream()
            .map(OrderInfo::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderDetailInfo getOrderDetail(String loginId, String password, Long orderId) {
        Member member = memberService.authenticate(loginId, password);
        Order order = orderService.getOrder(orderId);
        orderService.validateOwnership(member.getId(), order);
        return OrderDetailInfo.from(order);
    }

    @Transactional
    public OrderDetailInfo cancelOrder(String loginId, String password, Long orderId) {
        Member member = memberService.authenticate(loginId, password);

        // 1. 비관적 락 획득 + 취소 가능 여부 검증 (fail-fast)
        Order order = orderService.getOrderForUpdate(orderId);
        orderService.validateOwnership(member.getId(), order);
        order.cancel();

        // 2. 재고 복구 + 쿠폰 사용 취소
        restoreStockAndCancelCoupon(order, orderId);

        // 3. 취소된 주문 저장
        Order cancelledOrder = orderService.saveOrder(order);
        return OrderDetailInfo.from(cancelledOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderInfo> getOrdersForAdmin(String ldap, OrderPeriod period) {
        adminValidator.validate(ldap);
        LocalDateTime startDate = period != null ? period.getStartDate() : null;
        return orderService.getOrders(null, startDate)
            .stream()
            .map(OrderInfo::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderAdminDetailInfo getOrderDetailForAdmin(String ldap, Long orderId) {
        adminValidator.validate(ldap);
        Order order = orderService.getOrder(orderId);
        return OrderAdminDetailInfo.from(order);
    }

    @Transactional
    public OrderAdminDetailInfo changeOrderStatusForAdmin(String ldap, Long orderId, OrderStatus newStatus) {
        adminValidator.validate(ldap);

        if (newStatus == OrderStatus.CANCELLED) {
            // 1. 비관적 락 획득 + 취소 상태 전환 (fail-fast)
            Order order = orderService.getOrderForUpdate(orderId);
            order.transitionTo(newStatus);

            // 2. 재고 복구 + 쿠폰 사용 취소
            restoreStockAndCancelCoupon(order, orderId);

            // 3. 취소된 주문 저장
            Order updatedOrder = orderService.saveOrder(order);
            return OrderAdminDetailInfo.from(updatedOrder);
        }

        // 취소가 아닌 상태 변경은 기존 로직 유지
        Order updatedOrder = orderService.changeStatus(orderId, newStatus);
        return OrderAdminDetailInfo.from(updatedOrder);
    }

    private void restoreStockAndCancelCoupon(Order order, Long orderId) {
        // 재고 복구 — productId 오름차순 정렬 → 락 획득 순서 고정으로 데드락 방지
        List<OrderProduct> sortedProducts = order.getOrderProducts().stream()
            .sorted(Comparator.comparing(OrderProduct::getProductId))
            .toList();
        for (OrderProduct orderProduct : sortedProducts) {
            productService.increaseStock(
                orderProduct.getProductId(),
                orderProduct.getProductOptionId(),
                orderProduct.getQuantity()
            );
        }
        Set<Long> productIds = sortedProducts.stream()
            .map(OrderProduct::getProductId)
            .collect(Collectors.toSet());
        applicationEventPublisher.publishEvent(ProductCacheEvictEvent.of(productIds));
        // 쿠폰 사용 취소
        memberCouponService.cancelCouponUsage(orderId);
    }

    private Address findAddressForMember(Long memberId, Long addressId) {
        return addressService.getAddresses(memberId).stream()
            .filter(address -> address.getId().equals(addressId))
            .findFirst()
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "배송지를 찾을 수 없습니다."));
    }

    private String getThumbnailUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
            .filter(image -> image.getType() == ImageType.MAIN)
            .findFirst()
            .map(ProductImage::getUrl)
            .orElse(product.getImages().get(0).getUrl());
    }
}
