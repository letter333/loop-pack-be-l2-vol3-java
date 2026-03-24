package com.loopers.application.event;

import java.util.Set;

public record OrderCompletedEvent(
    Long orderId,
    Long memberId,
    Set<Long> productIds,
    Long totalAmount
) {
}
