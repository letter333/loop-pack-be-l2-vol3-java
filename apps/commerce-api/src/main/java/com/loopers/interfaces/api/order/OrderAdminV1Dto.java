package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderAdminDetailInfo;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderProductInfo;
import com.loopers.domain.order.OrderProductStatus;
import com.loopers.domain.order.OrderStatus;

import java.util.List;

public class OrderAdminV1Dto {

    public record OrderAdminResponse(
        Long id,
        String orderNumber,
        String orderName,
        OrderStatus status,
        Long totalAmount,
        Long paymentAmount,
        String thumbnailUrl
    ) {
        public static OrderAdminResponse from(OrderInfo info) {
            return new OrderAdminResponse(
                info.id(),
                info.orderNumber(),
                info.orderName(),
                info.status(),
                info.totalAmount(),
                info.paymentAmount(),
                info.thumbnailUrl()
            );
        }
    }

    public record OrderAdminDetailResponse(
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
        List<OrderProductResponse> orderProducts
    ) {
        public static OrderAdminDetailResponse from(OrderAdminDetailInfo info) {
            List<OrderProductResponse> products = info.orderProducts().stream()
                .map(OrderProductResponse::from)
                .toList();
            return new OrderAdminDetailResponse(
                info.id(),
                info.memberId(),
                info.orderNumber(),
                info.orderName(),
                info.status(),
                info.recipientName(),
                info.phone(),
                info.zipCode(),
                info.address(),
                info.addressDetail(),
                info.shippingMemo(),
                info.totalAmount(),
                info.shippingFee(),
                info.discountAmount(),
                info.paymentAmount(),
                products
            );
        }
    }

    public record OrderProductResponse(
        Long id,
        Long productId,
        Long productOptionId,
        String productName,
        String optionValue,
        Long price,
        Long extraPrice,
        int quantity,
        String thumbnailUrl,
        OrderProductStatus status
    ) {
        public static OrderProductResponse from(OrderProductInfo info) {
            return new OrderProductResponse(
                info.id(),
                info.productId(),
                info.productOptionId(),
                info.productName(),
                info.optionValue(),
                info.price(),
                info.extraPrice(),
                info.quantity(),
                info.thumbnailUrl(),
                info.status()
            );
        }
    }
}
