package com.loopers.domain.order;

public enum OrderProductStatus {
    NORMAL,             // 정상
    CANCEL_REQUESTED,   // 취소 요청
    CANCELLED,          // 취소 완료
    RETURN_REQUESTED,   // 반품 요청
    RETURNED            // 반품 완료
}
