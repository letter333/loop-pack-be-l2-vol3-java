package com.loopers.interfaces.api.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderDetailInfo;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderProductInfo;
import com.loopers.domain.order.OrderProductStatus;
import com.loopers.domain.order.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderV1Dto {

    public record CreateOrderRequest(
        @NotNull(message = "배송지 ID는 필수입니다.")
        Long addressId,
        String shippingMemo,
        @NotEmpty(message = "주문 상품은 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemRequest> items
    ) {
        public OrderCommand.Create toCommand() {
            List<OrderCommand.OrderItem> orderItems = items.stream()
                .map(item -> new OrderCommand.OrderItem(item.productId(), item.productOptionId(), item.quantity()))
                .toList();
            return new OrderCommand.Create(addressId, shippingMemo, orderItems);
        }
    }

    public record OrderItemRequest(
        @NotNull(message = "상품 ID는 필수입니다.")
        Long productId,
        @NotNull(message = "상품 옵션 ID는 필수입니다.")
        Long productOptionId,
        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        int quantity
    ) {
    }

    public record OrderResponse(
        Long id,
        String orderNumber,
        String orderName,
        OrderStatus status,
        Long totalAmount,
        Long paymentAmount,
        String thumbnailUrl
    ) {
        public static OrderResponse from(OrderInfo info) {
            return new OrderResponse(
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

    public record OrderDetailResponse(
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
        Long totalAmount,
        Long shippingFee,
        Long discountAmount,
        Long paymentAmount,
        List<OrderProductResponse> orderProducts
    ) {
        public static OrderDetailResponse from(OrderDetailInfo info) {
            List<OrderProductResponse> products = info.orderProducts().stream()
                .map(OrderProductResponse::from)
                .toList();
            return new OrderDetailResponse(
                info.id(),
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
        String optionName,
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
                info.optionName(),
                info.price(),
                info.extraPrice(),
                info.quantity(),
                info.thumbnailUrl(),
                info.status()
            );
        }
    }
}
