package com.loopers.domain.like;

import java.util.Optional;

public interface LikeRepository {

    Optional<Like> findByMemberIdAndTargetIdAndTargetType(Long memberId, Long targetId, TargetType targetType);

    Like save(Like like);

    void delete(Like like);
}
