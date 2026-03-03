package com.loopers.application.order;

import java.util.List;

public class OrderCommand {

    public record Create(
        Long addressId,
        String shippingMemo,
        List<OrderItem> items
    ) {
    }

    public record OrderItem(
        Long productId,
        Long productOptionId,
        int quantity
    ) {
    }
}
