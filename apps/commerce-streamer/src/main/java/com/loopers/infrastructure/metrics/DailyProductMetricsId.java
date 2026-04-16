package com.loopers.infrastructure.metrics;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyProductMetricsId implements Serializable {

    private Long productId;
    private LocalDate metricDate;

    public DailyProductMetricsId(Long productId, LocalDate metricDate) {
        this.productId = productId;
        this.metricDate = metricDate;
    }
}
