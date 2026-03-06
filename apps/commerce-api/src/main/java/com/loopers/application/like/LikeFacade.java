package com.loopers.application.like;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class LikeFacade {

    private final LikeService likeService;
    private final MemberService memberService;
    private final ProductService productService;
    private final BrandService brandService;

    @Transactional
    public LikeInfo toggleProductLike(String loginId, String password, Long productId) {
        Member member = memberService.authenticate(loginId, password);
        productService.getActiveProduct(productId);

        boolean liked = likeService.toggleLike(member.getId(), productId, TargetType.PRODUCT);

        Long likeCount;
        if (liked) {
            likeCount = productService.increaseLikeCount(productId);
        } else {
            likeCount = productService.decreaseLikeCount(productId);
        }

        return new LikeInfo(liked, likeCount);
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
