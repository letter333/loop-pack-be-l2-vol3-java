package com.loopers.application.queue;

import com.loopers.domain.queue.QueueService;
import com.loopers.domain.queue.QueueTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class QueueAdmissionScheduler {

    private static final String ORDER_EVENT_TYPE = "order";

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
        List<Long> userIds = queueService.dequeueBatch(ORDER_EVENT_TYPE, batchSize);

        if (userIds.isEmpty()) {
            return;
        }

        for (Long userId : userIds) {
            queueTokenService.issueToken(ORDER_EVENT_TYPE, userId);
        }

        log.info("대기열 입장 처리: {}명 토큰 발급", userIds.size());
    }
}
