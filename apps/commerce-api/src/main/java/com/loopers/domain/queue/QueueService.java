package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QueueService {

    private final QueueRepository queueRepository;

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
}
