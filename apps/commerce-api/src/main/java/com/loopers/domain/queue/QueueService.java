package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueueService {

    private final QueueRepository queueRepository;
    private final int batchSize;
    private final long intervalMs;

    public QueueService(
        QueueRepository queueRepository,
        @Value("${queue.admission.batch-size:50}") int batchSize,
        @Value("${queue.admission.interval-ms:10000}") long intervalMs
    ) {
        this.queueRepository = queueRepository;
        this.batchSize = batchSize;
        this.intervalMs = intervalMs;
    }

    public QueueInfo enter(String eventType, Long userId) {
        boolean added = queueRepository.addIfAbsent(eventType, userId, System.currentTimeMillis());
        if (!added) {
            throw new CoreException(ErrorType.CONFLICT, "이미 대기열에 진입한 상태입니다.");
        }

        Long position = queueRepository.getPosition(eventType, userId);
        long totalWaiting = queueRepository.getTotalWaiting(eventType);

        return new QueueInfo(position, totalWaiting);
    }

    public QueueInfo getQueueInfo(String eventType, Long userId) {
        Long position = queueRepository.getPosition(eventType, userId);
        if (position == null) {
            throw new CoreException(ErrorType.NOT_FOUND, "대기열에 존재하지 않는 유저입니다.");
        }

        long totalWaiting = queueRepository.getTotalWaiting(eventType);
        return new QueueInfo(position, totalWaiting);
    }

    public long getTotalWaiting(String eventType) {
        return queueRepository.getTotalWaiting(eventType);
    }

    public List<Long> dequeueBatch(String eventType, int count) {
        return queueRepository.popMin(eventType, count);
    }

    public long calculateEstimatedWaitSeconds(long position) {
        if (position <= 0) {
            return 0;
        }
        long batchCycles = (position + batchSize - 1) / batchSize;
        return batchCycles * intervalMs / 1000;
    }

    public long suggestPollIntervalMs(long position) {
        if (position <= 0) {
            return 1000;
        }
        if (position <= batchSize) {
            return 1000;
        }
        if (position <= batchSize * 3) {
            return 2000;
        }
        return 5000;
    }

    public void requeueAll(String eventType, List<Long> userIds) {
        queueRepository.requeueAll(eventType, userIds);
    }

    public void activateQueue(String eventType) {
        queueRepository.activate(eventType);
    }

    public void deactivateQueue(String eventType) {
        queueRepository.deactivate(eventType);
    }

    public boolean isQueueActive(String eventType) {
        return queueRepository.isActive(eventType);
    }
}
