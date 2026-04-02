package com.loopers.application.queue;

import com.loopers.domain.queue.QueueEventType;
import com.loopers.domain.queue.QueueService;
import com.loopers.domain.queue.QueueTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class QueueAdmissionScheduler {

    private final QueueService queueService;
    private final QueueTokenService queueTokenService;
    private final int batchSize;

    public QueueAdmissionScheduler(
        QueueService queueService,
        QueueTokenService queueTokenService,
        @Value("${queue.admission.batch-size:50}") int batchSize
    ) {
        this.queueService = queueService;
        this.queueTokenService = queueTokenService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${queue.admission.interval-ms:10000}")
    public void processAdmission() {
        List<Long> userIds = queueService.dequeueBatch(QueueEventType.ORDER, batchSize);

        if (userIds.isEmpty()) {
            return;
        }

        try {
            queueTokenService.issueTokensBatch(QueueEventType.ORDER, userIds);
            log.info("대기열 입장 처리: {}명 토큰 일괄 발급", userIds.size());
            return;
        } catch (Exception e) {
            log.warn("토큰 일괄 발급 실패, 개별 발급으로 전환", e);
        }

        int issued = 0;
        List<Long> failedUserIds = new ArrayList<>();
        for (Long userId : userIds) {
            try {
                queueTokenService.issueToken(QueueEventType.ORDER, userId);
                issued++;
            } catch (Exception e) {
                log.error("토큰 발급 실패 - userId: {}", userId, e);
                failedUserIds.add(userId);
            }
        }

        if (!failedUserIds.isEmpty()) {
            try {
                queueService.requeueAll(QueueEventType.ORDER, failedUserIds);
                log.warn("토큰 발급 실패 {}명 대기열 재삽입 완료", failedUserIds.size());
            } catch (Exception e) {
                log.error("대기열 재삽입 실패 - userIds: {}", failedUserIds, e);
            }
        }

        log.info("대기열 입장 처리: {}/{}명 토큰 발급", issued, userIds.size());
    }
}
