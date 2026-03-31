package com.loopers.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.domain.actionlog.UserActionLog;
import com.loopers.domain.actionlog.UserActionLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Slf4j
@Component
public class OrderEventListener {

    private final UserActionLogRepository userActionLogRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public OrderEventListener(UserActionLogRepository userActionLogRepository,
                              ObjectMapper objectMapper,
                              PlatformTransactionManager transactionManager) {
        this.userActionLogRepository = userActionLogRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        transactionTemplate.executeWithoutResult(status -> {
            UserActionLog actionLog = new UserActionLog(
                event.memberId(),
                ActionType.ORDER.name(),
                event.orderId(),
                "ORDER",
                toJson(Map.of("totalAmount", event.totalAmount()))
            );
            userActionLogRepository.save(actionLog);
        });
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("JSON 직렬화 실패", e);
        }
    }
}
