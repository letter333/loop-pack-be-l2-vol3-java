package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public record OrderAdminDetailInfo(
    Long id,
    Long memberId,
    String orderNumber,
    String orderName,
    OrderStatus status,
    String recipientName,
    String phone,
    String zipCode,
    String address,
    String addressDetail,
    String shippingMemo,
    Long totalAmount,
    Long shippingFee,
    Long discountAmount,
    Long paymentAmount,
    List<OrderProductInfo> orderProducts
) {

    public static OrderAdminDetailInfo from(Order order) {
        List<OrderProductInfo> products = order.getOrderProducts().stream()
            .map(OrderProductInfo::from)
            .toList();

        return new OrderAdminDetailInfo(
            order.getId(),
            order.getMemberId(),
            order.getOrderNumber(),
            order.getOrderName(),
            order.getStatus(),
            order.getRecipientName(),
            order.getPhone(),
            order.getZipCode(),
            order.getAddress(),
            order.getAddressDetail(),
            order.getShippingMemo(),
            order.getTotalAmount(),
            order.getShippingFee(),
            order.getDiscountAmount(),
            order.getPaymentAmount(),
            products
        );
    }
}
