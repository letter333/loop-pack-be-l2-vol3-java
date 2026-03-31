package com.loopers.application.queue;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.queue.QueueInfo;
import com.loopers.domain.queue.QueueService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueFacade {

    private static final String ORDER_EVENT_TYPE = "order";

    private final MemberService memberService;
    private final QueueService queueService;
    private final AdminValidator adminValidator;

    public QueueInfo enter(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        return queueService.enter(ORDER_EVENT_TYPE, member.getId());
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
