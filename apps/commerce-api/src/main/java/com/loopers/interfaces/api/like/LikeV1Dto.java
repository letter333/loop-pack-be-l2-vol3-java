package com.loopers.interfaces.api.like;

import com.loopers.application.like.LikeInfo;

public class LikeV1Dto {

    public record LikeResponse(
        boolean liked,
        Long likeCount
    ) {
        public static LikeResponse from(LikeInfo info) {
            return new LikeResponse(info.liked(), info.likeCount());
        }
    }
}
