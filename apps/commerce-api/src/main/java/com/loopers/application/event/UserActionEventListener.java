package com.loopers.application.event;

import com.loopers.domain.actionlog.UserActionLog;
import com.loopers.domain.actionlog.UserActionLogRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class UserActionEventListener {

    private final UserActionLogRepository userActionLogRepository;
    private final TransactionTemplate transactionTemplate;

    public UserActionEventListener(UserActionLogRepository userActionLogRepository,
                                   PlatformTransactionManager transactionManager) {
        this.userActionLogRepository = userActionLogRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserAction(UserActionEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            UserActionLog log = new UserActionLog(
                event.memberId(),
                event.actionType().name(),
                event.targetId(),
                event.targetType(),
                event.metadata()
            );
            userActionLogRepository.save(log);
        });
    }
}
