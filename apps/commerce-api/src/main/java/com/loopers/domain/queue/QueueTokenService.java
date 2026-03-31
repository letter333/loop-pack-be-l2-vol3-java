package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    public String getToken(String eventType, Long userId) {
        return queueTokenRepository.get(eventType, userId);
    }

    public long validateAndConsume(String eventType, Long userId, String token) {
        TokenConsumeResult result = queueTokenRepository.getAndDeleteWithTtl(eventType, userId);

        if (result.token() == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "유효한 대기열 토큰이 없습니다.");
        }
        if (!result.token().equals(token)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "토큰이 일치하지 않습니다.");
        }

        return result.ttlSeconds();
    }

    public void restoreToken(String eventType, Long userId, String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        queueTokenRepository.save(eventType, userId, token, Duration.ofSeconds(ttlSeconds));
    }
}
