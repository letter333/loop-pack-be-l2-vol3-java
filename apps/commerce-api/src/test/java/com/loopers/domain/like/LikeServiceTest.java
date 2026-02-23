package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@DisplayName("LikeService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @InjectMocks
    private LikeService likeService;

    @Mock
    private LikeRepository likeRepository;

    @Nested
    @DisplayName("toggleLike - 좋아요 토글")
    class ToggleLike {

        @Test
        @DisplayName("좋아요가 없으면 생성하고 true를 반환한다")
        void returnsTrue_whenLikeDoesNotExist() {
            // Arrange
            Long memberId = 1L;
            Long targetId = 100L;
            TargetType targetType = TargetType.PRODUCT;

            given(likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType))
                .willReturn(Optional.empty());
            given(likeRepository.save(any(Like.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = likeService.toggleLike(memberId, targetId, targetType);

            // Assert
            assertThat(result).isTrue();
            verify(likeRepository).save(any(Like.class));
            verify(likeRepository, never()).delete(any(Like.class));
        }

        @Test
        @DisplayName("좋아요가 있으면 삭제하고 false를 반환한다")
        void returnsFalse_whenLikeExists() {
            // Arrange
            Long memberId = 1L;
            Long targetId = 100L;
            TargetType targetType = TargetType.PRODUCT;
            Like existingLike = new Like(1L, memberId, targetId, targetType, null);

            given(likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType))
                .willReturn(Optional.of(existingLike));

            // Act
            boolean result = likeService.toggleLike(memberId, targetId, targetType);

            // Assert
            assertThat(result).isFalse();
            verify(likeRepository).delete(existingLike);
            verify(likeRepository, never()).save(any(Like.class));
        }

        @Test
        @DisplayName("BRAND 타입에서도 정상 동작한다")
        void worksWithBrandType() {
            // Arrange
            Long memberId = 1L;
            Long targetId = 50L;
            TargetType targetType = TargetType.BRAND;

            given(likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType))
                .willReturn(Optional.empty());
            given(likeRepository.save(any(Like.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

            // Act
            boolean result = likeService.toggleLike(memberId, targetId, targetType);

            // Assert
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("existsLike - 좋아요 존재 여부 확인")
    class ExistsLike {

        @Test
        @DisplayName("좋아요가 존재하면 true를 반환한다")
        void returnsTrue_whenLikeExists() {
            // Arrange
            Long memberId = 1L;
            Long targetId = 100L;
            TargetType targetType = TargetType.PRODUCT;
            Like existingLike = new Like(1L, memberId, targetId, targetType, null);

            given(likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType))
                .willReturn(Optional.of(existingLike));

            // Act
            boolean result = likeService.existsLike(memberId, targetId, targetType);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("좋아요가 존재하지 않으면 false를 반환한다")
        void returnsFalse_whenLikeDoesNotExist() {
            // Arrange
            Long memberId = 1L;
            Long targetId = 100L;
            TargetType targetType = TargetType.PRODUCT;

            given(likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType))
                .willReturn(Optional.empty());

            // Act
            boolean result = likeService.existsLike(memberId, targetId, targetType);

            // Assert
            assertThat(result).isFalse();
        }
    }
}
