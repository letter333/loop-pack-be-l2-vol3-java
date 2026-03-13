package com.loopers.domain.like;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
                try {
                    Like like = new Like(memberId, targetId, targetType);
                    likeRepository.save(like);
                    return true;
                } catch (DataIntegrityViolationException e) {
                    // 동시 요청으로 이미 좋아요가 생성된 경우 → 삭제로 토글 처리
                    likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType)
                        .ifPresent(likeRepository::delete);
                    return false;
                }
            });
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public boolean existsLike(Long memberId, Long targetId, TargetType targetType) {
        return likeRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType)
            .isPresent();
    }
}
