package com.loopers.application;

import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final ProductMetricsRepository productMetricsRepository;
    private final EventHandledRepository eventHandledRepository;

    @Transactional
    public void processEvent(String eventId, String eventType, String aggregateId) {
        try {
            eventHandledRepository.save(eventId);
        } catch (DataIntegrityViolationException e) {
            log.debug("이미 처리된 이벤트: eventId={}", eventId);
            return;
        }

        Long targetId = Long.parseLong(aggregateId);

        switch (eventType) {
            case "PRODUCT_LIKED" -> productMetricsRepository.incrementLikeCount(targetId, 1);
            case "PRODUCT_UNLIKED" -> productMetricsRepository.incrementLikeCount(targetId, -1);
            case "ORDER_COMPLETED" -> log.info("주문 완료 이벤트 수신: orderId={}", targetId);
            default -> log.warn("알 수 없는 이벤트 타입: eventType={}", eventType);
        }
    }
}
