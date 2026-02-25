package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public record OrderDetailInfo(
    Long id,
    String orderNumber,
    String orderName,
    OrderStatus status,
    String recipientName,
    String phone,
    String zipCode,
    String address,
    String addressDetail,
    String shippingMemo,
    int totalAmount,
    int shippingFee,
    int discountAmount,
    int paymentAmount,
    List<OrderProductInfo> orderProducts
) {

    public static OrderDetailInfo from(Order order) {
        List<OrderProductInfo> products = order.getOrderProducts().stream()
            .map(OrderProductInfo::from)
            .toList();

        return new OrderDetailInfo(
            order.getId(),
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
