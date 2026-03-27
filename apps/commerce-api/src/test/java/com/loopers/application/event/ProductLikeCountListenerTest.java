package com.loopers.application.event;

import com.loopers.domain.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@DisplayName("ProductLikeCountListener 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ProductLikeCountListenerTest {

    @InjectMocks
    private ProductLikeCountListener listener;

    @Mock
    private ProductService productService;

    @Test
    @DisplayName("ProductLikedEvent 수신 시 likeCount를 증가시킨다")
    void increasesLikeCount_whenProductLiked() {
        // Arrange
        ProductLikedEvent event = new ProductLikedEvent(1L);

        // Act
        listener.handleProductLiked(event);

        // Assert
        verify(productService).increaseLikeCount(1L);
    }

    @Test
    @DisplayName("ProductUnlikedEvent 수신 시 likeCount를 감소시킨다")
    void decreasesLikeCount_whenProductUnliked() {
        // Arrange
        ProductUnlikedEvent event = new ProductUnlikedEvent(1L);

        // Act
        listener.handleProductUnliked(event);

        // Assert
        verify(productService).decreaseLikeCount(1L);
    }
}
