package com.loopers.interfaces.api.queue;

import com.loopers.domain.queue.QueueInfo;

public class QueueV1Dto {

    public record EnterResponse(
        long position,
        long totalWaiting
    ) {
        public static EnterResponse from(QueueInfo info) {
            return new EnterResponse(info.position(), info.totalWaiting());
        }
    }

    public record QueueStatusResponse(
        boolean active
    ) {
    }
}
