package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.application.like.LikeInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class LikeV1Controller implements LikeV1ApiSpec {

    private final LikeFacade likeFacade;

    @PostMapping("/products/{productId}/like")
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> toggleProductLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long productId
    ) {
        LikeInfo info = likeFacade.toggleProductLike(loginId, password, productId);
        return ApiResponse.success(LikeV1Dto.LikeResponse.from(info));
    }

    @PostMapping("/brands/{brandId}/like")
    @Override
    public ApiResponse<LikeV1Dto.LikeResponse> toggleBrandLike(
        @RequestHeader("X-Loopers-LoginId") String loginId,
        @RequestHeader("X-Loopers-LoginPw") String password,
        @PathVariable Long brandId
    ) {
        LikeInfo info = likeFacade.toggleBrandLike(loginId, password, brandId);
        return ApiResponse.success(LikeV1Dto.LikeResponse.from(info));
    }
}
