package com.loopers.infrastructure.order;

import com.loopers.domain.order.OrderProduct;
import com.loopers.domain.order.OrderProductStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @Setter
    private OrderEntity order;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_option_id")
    private Long productOptionId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "option_value", length = 100)
    private String optionValue;

    @Column(name = "price", nullable = false)
    private Long price;

    @Column(name = "extra_price", nullable = false)
    private Long extraPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderProductStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public static OrderProductEntity from(OrderProduct orderProduct) {
        OrderProductEntity entity = new OrderProductEntity();
        entity.productId = orderProduct.getProductId();
        entity.productOptionId = orderProduct.getProductOptionId();
        entity.productName = orderProduct.getProductName();
        entity.optionValue = orderProduct.getOptionValue();
        entity.price = orderProduct.getPrice();
        entity.extraPrice = orderProduct.getExtraPrice();
        entity.quantity = orderProduct.getQuantity();
        entity.thumbnailUrl = orderProduct.getThumbnailUrl();
        entity.status = orderProduct.getStatus();
        return entity;
    }

    public OrderProduct toDomain() {
        return new OrderProduct(
            id,
            productId,
            productOptionId,
            productName,
            optionValue,
            price,
            extraPrice,
            quantity,
            thumbnailUrl,
            status
        );
    }

    public void updateStatus(OrderProductStatus status) {
        this.status = status;
    }
}
