package com.loopers.infrastructure.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderProduct;
import com.loopers.domain.order.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_member_id", columnList = "member_id"),
        @Index(name = "idx_orders_order_number", columnList = "order_number")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "order_number", nullable = false, unique = true, length = 20)
    private String orderNumber;

    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 20)
    private String phone;

    @Column(name = "recipient_zip_code", length = 10)
    private String zipCode;

    @Column(name = "recipient_address", nullable = false, length = 255)
    private String address;

    @Column(name = "recipient_address_detail", length = 255)
    private String addressDetail;

    @Column(name = "shipping_memo", length = 500)
    private String shippingMemo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private Long totalAmount;

    @Column(name = "shipping_fee", nullable = false)
    private Long shippingFee;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "payment_amount", nullable = false)
    private Long paymentAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderProductEntity> orderProducts = new ArrayList<>();

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

    public static OrderEntity from(Order order) {
        OrderEntity entity = new OrderEntity();
        entity.memberId = order.getMemberId();
        entity.orderNumber = order.getOrderNumber();
        entity.orderName = order.getOrderName();
        entity.recipientName = order.getRecipientName();
        entity.phone = order.getPhone();
        entity.zipCode = order.getZipCode();
        entity.address = order.getAddress();
        entity.addressDetail = order.getAddressDetail();
        entity.shippingMemo = order.getShippingMemo();
        entity.status = order.getStatus();
        entity.totalAmount = order.getTotalAmount();
        entity.shippingFee = order.getShippingFee();
        entity.discountAmount = order.getDiscountAmount();
        entity.paymentAmount = order.getPaymentAmount();

        if (order.getOrderProducts() != null) {
            for (OrderProduct orderProduct : order.getOrderProducts()) {
                entity.addOrderProduct(OrderProductEntity.from(orderProduct));
            }
        }

        return entity;
    }

    public void addOrderProduct(OrderProductEntity orderProduct) {
        orderProducts.add(orderProduct);
        orderProduct.setOrder(this);
    }

    public Order toDomain() {
        List<OrderProduct> domainOrderProducts = orderProducts.stream()
            .map(OrderProductEntity::toDomain)
            .toList();

        Order order = new Order(
            id,
            memberId,
            orderNumber,
            orderName,
            recipientName,
            phone,
            zipCode,
            address,
            addressDetail,
            shippingMemo,
            status,
            totalAmount,
            shippingFee,
            discountAmount,
            paymentAmount
        );
        order.setOrderProducts(domainOrderProducts);

        return order;
    }

    public void update(OrderStatus status) {
        this.status = status;
    }
}
