package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class Order {

    private static final long DEFAULT_SHIPPING_FEE = 3_000L;
    private static final long FREE_SHIPPING_THRESHOLD = 50_000L;

    private Long id;
    private Long memberId;
    private String orderNumber;
    private String orderName;
    private String recipientName;
    private String phone;
    private String zipCode;
    private String address;
    private String addressDetail;
    private String shippingMemo;
    private OrderStatus status;
    private Long totalAmount;
    private Long shippingFee;
    private Long discountAmount;
    private Long paymentAmount;
    private List<OrderProduct> orderProducts = new ArrayList<>();

    public Order(Long memberId, String recipientName, String phone, String zipCode,
                 String address, String addressDetail, String shippingMemo) {
        validateMemberId(memberId);

        this.memberId = memberId;
        this.orderNumber = generateOrderNumber();
        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.shippingMemo = shippingMemo;
        this.status = OrderStatus.PENDING;
        this.totalAmount = 0L;
        this.shippingFee = 0L;
        this.discountAmount = 0L;
        this.paymentAmount = 0L;
    }

    public Order(Long id, Long memberId, String orderNumber, String orderName,
                 String recipientName, String phone, String zipCode, String address,
                 String addressDetail, String shippingMemo, OrderStatus status,
                 Long totalAmount, Long shippingFee, Long discountAmount, Long paymentAmount) {
        this.id = id;
        this.memberId = memberId;
        this.orderNumber = orderNumber;
        this.orderName = orderName;
        this.recipientName = recipientName;
        this.phone = phone;
        this.zipCode = zipCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.shippingMemo = shippingMemo;
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingFee = shippingFee;
        this.discountAmount = discountAmount;
        this.paymentAmount = paymentAmount;
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        this.orderProducts.add(orderProduct);
        generateOrderName();
        calculateAmounts();
    }

    public void applyCouponDiscount(Long discountAmount) {
        if (discountAmount == null || discountAmount < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "유효하지 않은 할인 금액입니다.");
        }
        if (discountAmount > this.totalAmount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "할인 금액이 주문 금액을 초과할 수 없습니다.");
        }
        this.discountAmount = discountAmount;
        this.paymentAmount = this.totalAmount + this.shippingFee - this.discountAmount;
    }

    public void calculateAmounts() {
        this.totalAmount = orderProducts.stream()
            .mapToLong(OrderProduct::calculateTotalPrice)
            .sum();
        this.shippingFee = calculateShippingFee();
        this.paymentAmount = this.totalAmount + this.shippingFee - this.discountAmount;
    }

    private long calculateShippingFee() {
        return this.totalAmount >= FREE_SHIPPING_THRESHOLD ? 0L : DEFAULT_SHIPPING_FEE;
    }

    public boolean canCancel() {
        return this.status == OrderStatus.PENDING || this.status == OrderStatus.PAID;
    }

    public void cancel() {
        if (!canCancel()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "취소할 수 없는 주문 상태입니다.");
        }
        this.status = OrderStatus.CANCELLED;
        this.orderProducts.forEach(OrderProduct::cancel);
    }

    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this.status) {
            case PENDING -> newStatus == OrderStatus.PAID || newStatus == OrderStatus.CANCELLED;
            case PAID -> newStatus == OrderStatus.PREPARING || newStatus == OrderStatus.CANCELLED;
            case PREPARING -> newStatus == OrderStatus.SHIPPING;
            case SHIPPING -> newStatus == OrderStatus.DELIVERED;
            case DELIVERED -> newStatus == OrderStatus.RETURNED;
            case CANCELLED, RETURNED -> false;
        };
    }

    public void transitionTo(OrderStatus newStatus) {
        if (!canTransitionTo(newStatus)) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("'%s' 상태에서 '%s' 상태로 변경할 수 없습니다.", this.status, newStatus));
        }
        this.status = newStatus;
        if (newStatus == OrderStatus.CANCELLED) {
            this.orderProducts.forEach(OrderProduct::cancel);
        }
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }

    public void setOrderProducts(List<OrderProduct> orderProducts) {
        this.orderProducts = orderProducts;
    }

    private String generateOrderNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomSuffix = ThreadLocalRandom.current().nextInt(1000000, 10000000);
        return "ORD" + datePrefix + "-" + randomSuffix;
    }

    private void generateOrderName() {
        if (orderProducts.isEmpty()) {
            this.orderName = null;
            return;
        }
        String firstName = orderProducts.get(0).getProductName();
        if (orderProducts.size() == 1) {
            this.orderName = firstName;
        } else {
            this.orderName = firstName + " 외 " + (orderProducts.size() - 1) + "건";
        }
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }
}
