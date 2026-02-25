package com.loopers.application.order;

import com.loopers.domain.order.OrderProduct;
import com.loopers.domain.order.OrderProductStatus;

public record OrderProductInfo(
    Long id,
    Long productId,
    Long productOptionId,
    String productName,
    String optionName,
    int price,
    int extraPrice,
    int quantity,
    String thumbnailUrl,
    OrderProductStatus status
) {

    public static OrderProductInfo from(OrderProduct orderProduct) {
        return new OrderProductInfo(
            orderProduct.getId(),
            orderProduct.getProductId(),
            orderProduct.getProductOptionId(),
            orderProduct.getProductName(),
            orderProduct.getOptionName(),
            orderProduct.getPrice(),
            orderProduct.getExtraPrice(),
            orderProduct.getQuantity(),
            orderProduct.getThumbnailUrl(),
            orderProduct.getStatus()
        );
    }
}
