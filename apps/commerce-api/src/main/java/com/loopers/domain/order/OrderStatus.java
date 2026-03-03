package com.loopers.domain.order;

public enum OrderStatus {
    PENDING,      // 결제 대기
    PAID,         // 결제 완료
    PREPARING,    // 상품 준비중
    SHIPPING,     // 배송중
    DELIVERED,    // 배송 완료
    CANCELLED,    // 주문 취소
    RETURNED      // 반품 완료
}
