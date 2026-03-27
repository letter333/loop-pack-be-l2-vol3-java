package com.loopers.infrastructure.outbox;

import com.loopers.domain.outbox.OutboxEvent;
import com.loopers.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository outboxEventJpaRepository;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = OutboxEventEntity.from(event);
        return outboxEventJpaRepository.save(entity).toDomain();
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return outboxEventJpaRepository.findUnpublished(limit).stream()
            .map(OutboxEventEntity::toDomain)
            .toList();
    }

    @Override
    @Transactional
    public void markPublished(Long id) {
        outboxEventJpaRepository.markPublished(id);
    }
}
