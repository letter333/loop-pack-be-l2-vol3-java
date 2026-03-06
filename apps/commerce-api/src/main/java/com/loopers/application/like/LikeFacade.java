package com.loopers.application.like;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.like.TargetType;
import com.loopers.domain.member.Member;
import com.loopers.domain.member.MemberService;
import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class LikeFacade {

    private static final int MAX_RETRY_COUNT = 3;

    private final LikeService likeService;
    private final MemberService memberService;
    private final ProductService productService;
    private final BrandService brandService;
    private final TransactionTemplate transactionTemplate;

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

    public LikeInfo toggleBrandLike(String loginId, String password, Long brandId) {
        Member member = memberService.authenticate(loginId, password);
        brandService.getActiveBrand(brandId);

        int retryCount = 0;
        while (true) {
            try {
                return transactionTemplate.execute(status -> {
                    boolean liked = likeService.toggleLike(member.getId(), brandId, TargetType.BRAND);

                    Long likeCount;
                    if (liked) {
                        likeCount = brandService.increaseLikeCount(brandId);
                    } else {
                        likeCount = brandService.decreaseLikeCount(brandId);
                    }

                    return new LikeInfo(liked, likeCount);
                });
            } catch (ObjectOptimisticLockingFailureException e) {
                retryCount++;
                if (retryCount >= MAX_RETRY_COUNT) {
                    throw new CoreException(ErrorType.CONFLICT, "다른 요청과 충돌이 발생했습니다. 잠시 후 다시 시도해주세요.");
                }
            }
        }
    }
}
