package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderStatus;

public record OrderInfo(
    Long id,
    String orderNumber,
    String orderName,
    OrderStatus status,
    int totalAmount,
    int paymentAmount,
    String thumbnailUrl
) {

    public static OrderInfo from(Order order) {
        String thumbnailUrl = order.getOrderProducts().isEmpty()
            ? null
            : order.getOrderProducts().get(0).getThumbnailUrl();

        return new OrderInfo(
            order.getId(),
            order.getOrderNumber(),
            order.getOrderName(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getPaymentAmount(),
            thumbnailUrl
        );
    }
}
