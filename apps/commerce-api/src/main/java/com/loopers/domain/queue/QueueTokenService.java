package com.loopers.domain.queue;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueTokenService {

    private final QueueTokenRepository queueTokenRepository;

    @Value("${queue.token.ttl-minutes:5}")
    private long ttlMinutes;

    public String issueToken(String eventType, Long userId) {
        String token = UUID.randomUUID().toString();
        queueTokenRepository.save(eventType, userId, token, Duration.ofMinutes(ttlMinutes));
        return token;
    }

    public String getToken(String eventType, Long userId) {
        return queueTokenRepository.get(eventType, userId);
    }

    public long validateAndConsume(String eventType, Long userId, String token) {
        Long remainingTtl = queueTokenRepository.getExpire(eventType, userId);
        String storedToken = queueTokenRepository.getAndDelete(eventType, userId);

        if (storedToken == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "유효한 대기열 토큰이 없습니다.");
        }
        if (!storedToken.equals(token)) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "토큰이 일치하지 않습니다.");
        }

        return remainingTtl != null && remainingTtl > 0 ? remainingTtl : 0;
    }

    public void restoreToken(String eventType, Long userId, String token, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            return;
        }
        queueTokenRepository.save(eventType, userId, token, Duration.ofSeconds(ttlSeconds));
    }
}
