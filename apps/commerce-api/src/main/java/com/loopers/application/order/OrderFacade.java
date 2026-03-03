package com.loopers.application.order;

import com.loopers.application.coupon.CouponFacade;
import com.loopers.domain.address.Address;
import com.loopers.domain.address.AddressService;
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
import com.loopers.support.auth.AdminValidator;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;
    private final MemberService memberService;
    private final AddressService addressService;
    private final ProductService productService;
    private final CouponFacade couponFacade;
    private final AdminValidator adminValidator;

    @Transactional
    public OrderDetailInfo createOrder(String loginId, String password, OrderCommand.Create command) {
        Member member = memberService.authenticate(loginId, password);

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

        for (OrderCommand.OrderItem item : command.items()) {
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

        // 쿠폰 적용
        if (command.memberCouponId() != null) {
            Long discountAmount = couponFacade.calculateCouponDiscount(
                command.memberCouponId(), member.getId(), order.getTotalAmount()
            );
            order.applyCouponDiscount(discountAmount);
        }

        Order savedOrder = orderService.createOrder(order);

        // 주문 저장 후 쿠폰 사용 처리
        if (command.memberCouponId() != null) {
            couponFacade.applyCoupon(command.memberCouponId(), savedOrder.getId());
        }

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
        Order order = orderService.getOrder(orderId);
        orderService.validateOwnership(member.getId(), order);

        // 1. 재고 복구 (먼저 - 실패 시 전체 롤백)
        for (OrderProduct orderProduct : order.getOrderProducts()) {
            productService.increaseStock(
                orderProduct.getProductId(),
                orderProduct.getProductOptionId(),
                orderProduct.getQuantity()
            );
        }

        // 2. 쿠폰 사용 취소
        couponFacade.cancelCouponUsage(orderId);

        // 3. 주문 취소 (이후)
        Order cancelledOrder = orderService.cancelOrder(orderId);
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
        Order order = orderService.getOrder(orderId);

        // 1. 취소 시 재고 복구 (먼저 - 실패 시 전체 롤백)
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderProduct op : order.getOrderProducts()) {
                productService.increaseStock(op.getProductId(), op.getProductOptionId(), op.getQuantity());
            }
            // 쿠폰 사용 취소
            couponFacade.cancelCouponUsage(orderId);
        }

        // 2. 상태 변경 (이후)
        Order updatedOrder = orderService.changeStatus(orderId, newStatus);
        return OrderAdminDetailInfo.from(updatedOrder);
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
