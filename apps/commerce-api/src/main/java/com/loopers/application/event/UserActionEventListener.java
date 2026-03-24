package com.loopers.application.event;

import com.loopers.domain.actionlog.UserActionLog;
import com.loopers.domain.actionlog.UserActionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserActionEventListener {

    private final UserActionLogRepository userActionLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUserAction(UserActionEvent event) {
        UserActionLog log = new UserActionLog(
            event.memberId(),
            event.actionType().name(),
            event.targetId(),
            event.targetType(),
            event.metadata()
        );
        userActionLogRepository.save(log);
    }
}
