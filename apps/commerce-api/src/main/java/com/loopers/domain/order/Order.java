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
    private int totalAmount;
    private int shippingFee;
    private int discountAmount;
    private int paymentAmount;
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
        this.totalAmount = 0;
        this.shippingFee = 0;
        this.discountAmount = 0;
        this.paymentAmount = 0;
    }

    public Order(Long id, Long memberId, String orderNumber, String orderName,
                 String recipientName, String phone, String zipCode, String address,
                 String addressDetail, String shippingMemo, OrderStatus status,
                 int totalAmount, int shippingFee, int discountAmount, int paymentAmount) {
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

    public void setShippingFee(int shippingFee) {
        this.shippingFee = shippingFee;
    }

    public void setDiscountAmount(int discountAmount) {
        this.discountAmount = discountAmount;
    }

    public void calculateAmounts() {
        this.totalAmount = orderProducts.stream()
            .mapToInt(OrderProduct::calculateTotalPrice)
            .sum();
        this.paymentAmount = this.totalAmount + this.shippingFee - this.discountAmount;
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
