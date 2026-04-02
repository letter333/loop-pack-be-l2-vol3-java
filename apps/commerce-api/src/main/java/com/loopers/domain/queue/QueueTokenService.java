package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QueueTokenService {

    private final QueueTokenRepository queueTokenRepository;
    private final long ttlMinutes;

    public QueueTokenService(
        QueueTokenRepository queueTokenRepository,
        @Value("${queue.token.ttl-minutes:5}") long ttlMinutes
    ) {
        this.queueTokenRepository = queueTokenRepository;
        this.ttlMinutes = ttlMinutes;
    }

    public String issueToken(String eventType, Long userId) {
        String token = UUID.randomUUID().toString();
        queueTokenRepository.save(eventType, userId, token, Duration.ofMinutes(ttlMinutes));
        return token;
    }

    public void issueTokensBatch(String eventType, List<Long> userIds) {
        Map<Long, String> userTokens = new LinkedHashMap<>();
        for (Long userId : userIds) {
            userTokens.put(userId, UUID.randomUUID().toString());
        }
        queueTokenRepository.saveBatch(eventType, userTokens, Duration.ofMinutes(ttlMinutes));
    }

    public String getToken(String eventType, Long userId) {
        return queueTokenRepository.get(eventType, userId);
    }

    public long validateAndConsume(String eventType, Long userId, String token) {
        TokenConsumeResult result = queueTokenRepository.consumeIfMatches(eventType, userId, token);

        return switch (result.status()) {
            case CONSUMED -> result.ttlSeconds();
            case NOT_FOUND -> throw new CoreException(ErrorType.UNAUTHORIZED, "유효한 대기열 토큰이 없습니다.");
            case MISMATCH -> throw new CoreException(ErrorType.UNAUTHORIZED, "토큰이 일치하지 않습니다.");
        };
    }

    public void restoreToken(String eventType, Long userId, String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        queueTokenRepository.save(eventType, userId, token, Duration.ofSeconds(ttlSeconds));
    }
}
