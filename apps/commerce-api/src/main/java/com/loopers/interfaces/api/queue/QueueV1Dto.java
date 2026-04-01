package com.loopers.interfaces.api.queue;

import com.loopers.domain.queue.QueueInfo;
import com.loopers.domain.queue.QueuePositionInfo;

public class QueueV1Dto {

    public record EnterResponse(
        long position,
        long totalWaiting
    ) {
        public static EnterResponse from(QueueInfo info) {
            return new EnterResponse(info.position(), info.totalWaiting());
        }
    }

    public record PositionResponse(
        String status,
        long position,
        long totalWaiting,
        long estimatedWaitSeconds,
        String token
    ) {
        public static PositionResponse from(QueuePositionInfo info) {
            return new PositionResponse(
                info.status(), info.position(), info.totalWaiting(),
                info.estimatedWaitSeconds(), info.token()
            );
        }
    }

    public record QueueStatusResponse(
        boolean active
    ) {
    }
}
