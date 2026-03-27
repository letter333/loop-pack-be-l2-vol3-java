package com.loopers.application.coupon;

public record CouponIssueRequestStatusInfo(
    String requestId,
    String status,
    Long couponId
) {

    public static CouponIssueRequestStatusInfo of(String requestId, String status, Long couponId) {
        return new CouponIssueRequestStatusInfo(requestId, status, couponId);
    }

    public static CouponIssueRequestStatusInfo pending(String requestId) {
        return new CouponIssueRequestStatusInfo(requestId, "PENDING", null);
    }
}
