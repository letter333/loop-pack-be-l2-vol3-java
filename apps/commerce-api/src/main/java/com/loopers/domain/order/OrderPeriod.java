package com.loopers.domain.order;

import java.time.LocalDateTime;

public enum OrderPeriod {
    THREE_MONTHS(3),
    SIX_MONTHS(6),
    ONE_YEAR(12),
    ALL(null);

    private final Integer months;

    OrderPeriod(Integer months) {
        this.months = months;
    }

    public LocalDateTime getStartDate() {
        if (months == null) {
            return null;
        }
        return LocalDateTime.now().minusMonths(months);
    }
}
