package com.loopers.application.queue;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.queue.QueueInfo;
import com.loopers.domain.queue.QueuePositionInfo;
import com.loopers.domain.queue.QueueService;
import com.loopers.domain.queue.QueueTokenService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueFacade {

    private static final String ORDER_EVENT_TYPE = "order";

    private final MemberService memberService;
    private final QueueService queueService;
    private final QueueTokenService queueTokenService;
    private final AdminValidator adminValidator;

    public QueueInfo enter(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        return queueService.enter(ORDER_EVENT_TYPE, member.getId());
    }

    public QueuePositionInfo getPosition(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        Long userId = member.getId();

        String token = queueTokenService.getToken(ORDER_EVENT_TYPE, userId);
        if (token != null) {
            long totalWaiting = queueService.getTotalWaiting(ORDER_EVENT_TYPE);
            return new QueuePositionInfo("ADMITTED", 0, totalWaiting, 0, 0, token);
        }

        QueueInfo queueInfo = queueService.getQueueInfo(ORDER_EVENT_TYPE, userId);
        long estimatedWait = queueService.calculateEstimatedWaitSeconds(queueInfo.position());
        long pollInterval = queueService.suggestPollIntervalMs(queueInfo.position());
        return new QueuePositionInfo("WAITING", queueInfo.position(), queueInfo.totalWaiting(), estimatedWait, pollInterval, null);
    }

    public void activateQueue(String ldap) {
        adminValidator.validate(ldap);
        queueService.activateQueue(ORDER_EVENT_TYPE);
    }

    public void deactivateQueue(String ldap) {
        adminValidator.validate(ldap);
        queueService.deactivateQueue(ORDER_EVENT_TYPE);
    }

    public boolean isQueueActive(String ldap) {
        adminValidator.validate(ldap);
        return queueService.isQueueActive(ORDER_EVENT_TYPE);
    }
}
