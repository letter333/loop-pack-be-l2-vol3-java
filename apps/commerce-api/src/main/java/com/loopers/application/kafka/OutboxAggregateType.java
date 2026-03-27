package com.loopers.application.kafka;

public final class OutboxAggregateType {

    private OutboxAggregateType() {}

    public static final String PRODUCT = "PRODUCT";
    public static final String ORDER = "ORDER";
    public static final String COUPON = "COUPON";
}
