package com.loopers.application.like;

import com.loopers.application.event.ActionType;
import com.loopers.application.event.ProductLikedEvent;
import com.loopers.application.event.ProductUnlikedEvent;
import com.loopers.application.event.UserActionEvent;
import com.loopers.application.product.ProductCacheEvictEvent;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeService likeService;
    private final MemberService memberService;
    private final ProductService productService;
    private final BrandService brandService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public LikeInfo toggleProductLike(String loginId, String password, Long productId) {
        Member member = memberService.authenticate(loginId, password);
        Product product = productService.getActiveProduct(productId);

        boolean liked = likeService.toggleLike(member.getId(), productId, TargetType.PRODUCT);

        // 낙관적 count ±1: 현재 DB 값 기준으로 계산하여 즉시 반환
        Long currentCount = product.getLikeCount();
        Long expectedCount = liked ? currentCount + 1 : Math.max(0, currentCount - 1);

        // 실제 likeCount 업데이트는 AFTER_COMMIT 리스너에서 별도 트랜잭션으로 처리 (Eventual Consistency)
        if (liked) {
            applicationEventPublisher.publishEvent(new ProductLikedEvent(productId));
        } else {
            applicationEventPublisher.publishEvent(new ProductUnlikedEvent(productId));
        }

        applicationEventPublisher.publishEvent(ProductCacheEvictEvent.of(productId));

        // 부가 로직: 유저 행동 로깅
        applicationEventPublisher.publishEvent(new UserActionEvent(
            member.getId(), ActionType.LIKE, productId, "PRODUCT", null
        ));

        return new LikeInfo(liked, expectedCount);
    }

    @Transactional
    public LikeInfo toggleBrandLike(String loginId, String password, Long brandId) {
        Member member = memberService.authenticate(loginId, password);
        brandService.getActiveBrand(brandId);

        boolean liked = likeService.toggleLike(member.getId(), brandId, TargetType.BRAND);

        Long likeCount;
        if (liked) {
            likeCount = brandService.increaseLikeCount(brandId);
        } else {
            likeCount = brandService.decreaseLikeCount(brandId);
        }

        return new LikeInfo(liked, likeCount);
    }
}
