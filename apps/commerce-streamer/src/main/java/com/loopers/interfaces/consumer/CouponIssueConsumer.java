package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.CouponIssueService;
import com.loopers.confg.kafka.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponIssueConsumer {

    private final CouponIssueService couponIssueService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
        topics = {"coupon-issue-requests"},
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void handleCouponIssueRequests(
        List<ConsumerRecord<Object, Object>> messages,
        Acknowledgment acknowledgment
    ) {
        int failCount = 0;
        for (ConsumerRecord<Object, Object> message : messages) {
            try {
                JsonNode node = objectMapper.readTree(message.value().toString());
                String requestId = node.get("requestId").asText();
                Long memberId = node.get("memberId").asLong();
                Long couponId = node.get("couponId").asLong();

                couponIssueService.processCouponIssue(requestId, memberId, couponId);
            } catch (Exception e) {
                failCount++;
                log.error("coupon-issue-requests 메시지 처리 실패: partition={}, offset={}, error={}",
                    message.partition(), message.offset(), e.getMessage());
            }
        }
        if (failCount > 0) {
            log.warn("coupon-issue-requests 배치 처리 완료: total={}, failed={}", messages.size(), failCount);
        }
        acknowledgment.acknowledge();
    }
}
