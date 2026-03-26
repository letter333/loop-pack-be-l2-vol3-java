package com.loopers.application;

import com.loopers.domain.eventhandled.EventHandledRepository;
import com.loopers.domain.metrics.ProductMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("MetricsService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @InjectMocks
    private MetricsService metricsService;

    @Mock
    private ProductMetricsRepository productMetricsRepository;

    @Mock
    private EventHandledRepository eventHandledRepository;

    @Test
    @DisplayName("PRODUCT_LIKED 이벤트 수신 시 likeCount를 +1 한다")
    void incrementsLikeCount_whenProductLiked() {
        // Arrange
        given(eventHandledRepository.existsByEventId("1")).willReturn(false);

        // Act
        metricsService.processEvent("1", "PRODUCT_LIKED", "100");

        // Assert
        verify(productMetricsRepository).incrementLikeCount(100L, 1);
        verify(eventHandledRepository).save("1");
    }

    @Test
    @DisplayName("PRODUCT_UNLIKED 이벤트 수신 시 likeCount를 -1 한다")
    void decrementsLikeCount_whenProductUnliked() {
        // Arrange
        given(eventHandledRepository.existsByEventId("2")).willReturn(false);

        // Act
        metricsService.processEvent("2", "PRODUCT_UNLIKED", "100");

        // Assert
        verify(productMetricsRepository).incrementLikeCount(100L, -1);
        verify(eventHandledRepository).save("2");
    }

    @Test
    @DisplayName("이미 처리된 이벤트는 skip 한다")
    void skips_whenEventAlreadyHandled() {
        // Arrange
        given(eventHandledRepository.existsByEventId("3")).willReturn(true);

        // Act
        metricsService.processEvent("3", "PRODUCT_LIKED", "100");

        // Assert
        verify(productMetricsRepository, never()).incrementLikeCount(100L, 1);
        verify(eventHandledRepository, never()).save("3");
    }
}
