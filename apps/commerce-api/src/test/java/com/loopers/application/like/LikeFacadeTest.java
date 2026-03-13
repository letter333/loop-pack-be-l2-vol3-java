package com.loopers.application.like;

import com.loopers.application.product.ProductDetailCacheRepository;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@DisplayName("LikeFacade 단위 테스트")
@ExtendWith(MockitoExtension.class)
class LikeFacadeTest {

    @InjectMocks
    private LikeFacade likeFacade;

    @Mock
    private LikeService likeService;

    @Mock
    private MemberService memberService;

    @Mock
    private ProductService productService;

    @Mock
    private BrandService brandService;

    @Mock
    private ProductDetailCacheRepository productDetailCacheRepository;

    @Nested
    @DisplayName("toggleProductLike - 상품 좋아요 토글")
    class ToggleProductLike {

        @Test
        @DisplayName("인증 실패 시 UNAUTHORIZED 예외가 발생한다")
        void throwsUnauthorized_whenAuthenticationFails() {
            // Arrange
            String loginId = "user1";
            String password = "wrongPassword";
            Long productId = 100L;

            given(memberService.authenticate(loginId, password))
                .willThrow(new CoreException(ErrorType.UNAUTHORIZED));

            // Act & Assert
            assertThatThrownBy(() -> likeFacade.toggleProductLike(loginId, password, productId))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED));
        }

        @Test
        @DisplayName("상품이 존재하지 않으면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenProductDoesNotExist() {
            // Arrange
            String loginId = "user1";
            String password = "password123";
            Long productId = 999L;
            Member member = createMember(1L, loginId);

            given(memberService.authenticate(loginId, password)).willReturn(member);
            given(productService.getActiveProduct(productId))
                .willThrow(new CoreException(ErrorType.NOT_FOUND));

            // Act & Assert
            assertThatThrownBy(() -> likeFacade.toggleProductLike(loginId, password, productId))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("좋아요 생성 시 liked=true와 증가된 likeCount를 반환한다")
        void returnsLikedTrueAndIncreasedCount_whenLikeCreated() {
            // Arrange
            String loginId = "user1";
            String password = "password123";
            Long productId = 100L;
            Long memberId = 1L;
            Member member = createMember(memberId, loginId);
            Product product = createProduct(productId);

            given(memberService.authenticate(loginId, password)).willReturn(member);
            given(productService.getActiveProduct(productId)).willReturn(product);
            given(likeService.toggleLike(memberId, productId, TargetType.PRODUCT)).willReturn(true);
            given(productService.increaseLikeCount(productId)).willReturn(1L);

            // Act
            LikeInfo result = likeFacade.toggleProductLike(loginId, password, productId);

            // Assert
            assertThat(result.liked()).isTrue();
            assertThat(result.likeCount()).isEqualTo(1L);
            verify(productService).increaseLikeCount(productId);
        }

        @Test
        @DisplayName("좋아요 삭제 시 liked=false와 감소된 likeCount를 반환한다")
        void returnsLikedFalseAndDecreasedCount_whenLikeDeleted() {
            // Arrange
            String loginId = "user1";
            String password = "password123";
            Long productId = 100L;
            Long memberId = 1L;
            Member member = createMember(memberId, loginId);
            Product product = createProduct(productId);

            given(memberService.authenticate(loginId, password)).willReturn(member);
            given(productService.getActiveProduct(productId)).willReturn(product);
            given(likeService.toggleLike(memberId, productId, TargetType.PRODUCT)).willReturn(false);
            given(productService.decreaseLikeCount(productId)).willReturn(0L);

            // Act
            LikeInfo result = likeFacade.toggleProductLike(loginId, password, productId);

            // Assert
            assertThat(result.liked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0L);
            verify(productService).decreaseLikeCount(productId);
        }
    }

    @Nested
    @DisplayName("toggleBrandLike - 브랜드 좋아요 토글")
    class ToggleBrandLike {

        @Test
        @DisplayName("인증 실패 시 UNAUTHORIZED 예외가 발생한다")
        void throwsUnauthorized_whenAuthenticationFails() {
            // Arrange
            String loginId = "user1";
            String password = "wrongPassword";
            Long brandId = 50L;

            given(memberService.authenticate(loginId, password))
                .willThrow(new CoreException(ErrorType.UNAUTHORIZED));

            // Act & Assert
            assertThatThrownBy(() -> likeFacade.toggleBrandLike(loginId, password, brandId))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED));
        }

        @Test
        @DisplayName("브랜드가 존재하지 않으면 NOT_FOUND 예외가 발생한다")
        void throwsNotFound_whenBrandDoesNotExist() {
            // Arrange
            String loginId = "user1";
            String password = "password123";
            Long brandId = 999L;
            Member member = createMember(1L, loginId);

            given(memberService.authenticate(loginId, password)).willReturn(member);
            given(brandService.getActiveBrand(brandId))
                .willThrow(new CoreException(ErrorType.NOT_FOUND));

            // Act & Assert
            assertThatThrownBy(() -> likeFacade.toggleBrandLike(loginId, password, brandId))
                .isInstanceOf(CoreException.class)
                .satisfies(ex -> assertThat(((CoreException) ex).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
        }

        @Test
        @DisplayName("좋아요 생성 시 liked=true와 증가된 likeCount를 반환한다")
        void returnsLikedTrueAndIncreasedCount_whenLikeCreated() {
            // Arrange
            String loginId = "user1";
            String password = "password123";
            Long brandId = 50L;
            Long memberId = 1L;
            Member member = createMember(memberId, loginId);
            Brand brand = createBrand(brandId);

            given(memberService.authenticate(loginId, password)).willReturn(member);
            given(brandService.getActiveBrand(brandId)).willReturn(brand);
            given(likeService.toggleLike(memberId, brandId, TargetType.BRAND)).willReturn(true);
            given(brandService.increaseLikeCount(brandId)).willReturn(1L);

            // Act
            LikeInfo result = likeFacade.toggleBrandLike(loginId, password, brandId);

            // Assert
            assertThat(result.liked()).isTrue();
            assertThat(result.likeCount()).isEqualTo(1L);
            verify(brandService).increaseLikeCount(brandId);
        }

        @Test
        @DisplayName("좋아요 삭제 시 liked=false와 감소된 likeCount를 반환한다")
        void returnsLikedFalseAndDecreasedCount_whenLikeDeleted() {
            // Arrange
            String loginId = "user1";
            String password = "password123";
            Long brandId = 50L;
            Long memberId = 1L;
            Member member = createMember(memberId, loginId);
            Brand brand = createBrand(brandId);

            given(memberService.authenticate(loginId, password)).willReturn(member);
            given(brandService.getActiveBrand(brandId)).willReturn(brand);
            given(likeService.toggleLike(memberId, brandId, TargetType.BRAND)).willReturn(false);
            given(brandService.decreaseLikeCount(brandId)).willReturn(0L);

            // Act
            LikeInfo result = likeFacade.toggleBrandLike(loginId, password, brandId);

            // Assert
            assertThat(result.liked()).isFalse();
            assertThat(result.likeCount()).isEqualTo(0L);
            verify(brandService).decreaseLikeCount(brandId);
        }
    }

    private Member createMember(Long id, String loginId) {
        return new Member(id, loginId, "encodedPassword", "Test User",
            LocalDate.of(1990, 1, 1), "test@example.com");
    }

    private Product createProduct(Long id) {
        return new Product(id, "Test Product", "20240101-00001", 1L, 1L, 10000L,
            com.loopers.domain.product.ProductStatus.SALE, null, null, 0L,
            null, null, null);
    }

    private Brand createBrand(Long id) {
        return new Brand(id, "Test Brand", "Description", "https://example.com/logo.png", 0L,
            0L, null, null, null);
    }
}
