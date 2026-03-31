package com.loopers.application.queue;

import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.queue.QueueInfo;
import com.loopers.domain.queue.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QueueFacade {

    private static final String ORDER_EVENT_TYPE = "order";

    private final MemberService memberService;
    private final QueueService queueService;

    public QueueInfo enter(String loginId, String password) {
        Member member = memberService.authenticate(loginId, password);
        return queueService.enter(ORDER_EVENT_TYPE, member.getId());
    }
}
