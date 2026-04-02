package com.loopers.application.queue;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.queue.QueueEventType;
import com.loopers.domain.queue.QueueInfo;
import com.loopers.domain.queue.QueuePositionInfo;
import com.loopers.domain.queue.QueuePositionStatus;
import com.loopers.domain.queue.QueueService;
import com.loopers.domain.queue.QueueTokenService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueFacade {

    private final MemberService memberService;
    private final QueueService queueService;
    private final QueueTokenService queueTokenService;
    private final AdminValidator adminValidator;

    public QueueInfo enter(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        return queueService.enter(QueueEventType.ORDER, member.getId());
    }

    public QueuePositionInfo getPosition(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        Long userId = member.getId();

        String token = queueTokenService.getToken(QueueEventType.ORDER, userId);
        if (token != null) {
            long totalWaiting = queueService.getTotalWaiting(QueueEventType.ORDER);
            return new QueuePositionInfo(QueuePositionStatus.ADMITTED, 0, totalWaiting, 0, 0, token);
        }

        QueueInfo queueInfo = queueService.getQueueInfo(QueueEventType.ORDER, userId);
        long estimatedWait = queueService.calculateEstimatedWaitSeconds(queueInfo.position());
        long pollInterval = queueService.suggestPollIntervalMs(queueInfo.position());
        return new QueuePositionInfo(QueuePositionStatus.WAITING, queueInfo.position(), queueInfo.totalWaiting(), estimatedWait, pollInterval, null);
    }

    public void activateQueue(String ldap) {
        adminValidator.validate(ldap);
        queueService.activateQueue(QueueEventType.ORDER);
    }

    public void deactivateQueue(String ldap) {
        adminValidator.validate(ldap);
        queueService.deactivateQueue(QueueEventType.ORDER);
    }

    public boolean isQueueActive(String ldap) {
        adminValidator.validate(ldap);
        return queueService.isQueueActive(QueueEventType.ORDER);
    }
}
