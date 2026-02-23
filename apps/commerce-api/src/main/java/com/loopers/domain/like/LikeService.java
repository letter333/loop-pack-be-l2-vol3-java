package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeRepository likeRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public boolean toggleLike(Long memberId, Long targetId, TargetType targetType) {
        return likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType)
            .map(existingLike -> {
                likeRepository.delete(existingLike);
                return false;
            })
            .orElseGet(() -> {
                Like like = new Like(memberId, targetId, targetType);
                likeRepository.save(like);
                return true;
            });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public boolean existsLike(Long memberId, Long targetId, TargetType targetType) {
        return likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType)
            .isPresent();
    }
}
