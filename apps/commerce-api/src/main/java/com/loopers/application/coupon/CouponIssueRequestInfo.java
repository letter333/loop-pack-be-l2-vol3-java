package com.loopers.application.coupon;

public record CouponIssueRequestInfo(
    String requestId,
    Long couponId,
    String status
) {

    public static CouponIssueRequestInfo pending(String requestId, Long couponId) {
        return new CouponIssueRequestInfo(requestId, couponId, "PENDING");
    }
}
