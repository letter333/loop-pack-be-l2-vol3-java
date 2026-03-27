package com.loopers.domain.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    /**
     * 도메인 트랜잭션 내에서 호출하여 Outbox 이벤트를 기록한다.
     * 같은 트랜잭션에서 실행되므로 비즈니스 데이터와 원자적으로 저장된다.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent recordEvent(String aggregateType, String aggregateId,
                                   String eventType, String payload) {
        OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, payload);
        return outboxEventRepository.save(event);
    }
}
