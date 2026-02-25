package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

@Getter
public class OrderProduct {

    private Long id;
    private Long productId;
    private Long productOptionId;
    private String productName;
    private String optionValue;
    private Long price;
    private Long extraPrice;
    private int quantity;
    private String thumbnailUrl;
    private OrderProductStatus status;

    public OrderProduct(Long productId, Long productOptionId, String productName, String optionValue,
                        Long price, Long extraPrice, int quantity, String thumbnailUrl) {
        validateQuantity(quantity);

        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionValue = optionValue;
        this.price = price;
        this.extraPrice = extraPrice;
        this.quantity = quantity;
        this.thumbnailUrl = thumbnailUrl;
        this.status = OrderProductStatus.NORMAL;
    }

    public OrderProduct(Long id, Long productId, Long productOptionId, String productName, String optionValue,
                        Long price, Long extraPrice, int quantity, String thumbnailUrl, OrderProductStatus status) {
        this.id = id;
        this.productId = productId;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionValue = optionValue;
        this.price = price;
        this.extraPrice = extraPrice;
        this.quantity = quantity;
        this.thumbnailUrl = thumbnailUrl;
        this.status = status;
    }

    public Long calculateTotalPrice() {
        return (price + extraPrice) * quantity;
    }

    public void cancel() {
        if (this.status == OrderProductStatus.CANCELLED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문 상품입니다.");
        }
        this.status = OrderProductStatus.CANCELLED;
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "수량은 1개 이상이어야 합니다.");
        }
    }
}
