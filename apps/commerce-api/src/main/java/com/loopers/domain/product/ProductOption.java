package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProductOption {

    private Long id;
    private Long productId;
    private String optionValue;
    private String displayName;
    private Long extraPrice;
    private Integer stockQuantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public ProductOption(Long productId, String optionValue, String displayName, Long extraPrice, Integer stockQuantity) {
        ProductOptionValidator.validateOptionValue(optionValue);
        ProductOptionValidator.validateStockQuantity(stockQuantity);

        this.productId = productId;
        this.optionValue = optionValue;
        this.displayName = displayName;
        this.extraPrice = extraPrice != null ? extraPrice : 0L;
        this.stockQuantity = stockQuantity;
    }

    public ProductOption(Long id, Long productId, String optionValue, String displayName, Long extraPrice, Integer stockQuantity,
                         LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.productId = productId;
        this.optionValue = optionValue;
        this.displayName = displayName;
        this.extraPrice = extraPrice;
        this.stockQuantity = stockQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.");
        }
        if (this.stockQuantity < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.stockQuantity -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "증가 수량은 1 이상이어야 합니다.");
        }
        this.stockQuantity += quantity;
    }
}