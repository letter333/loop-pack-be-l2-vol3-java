package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.like.TargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryImpl implements LikeRepository {

    private final LikeJpaRepository likeJpaRepository;

    @Override
    public Optional<Like> findByMemberIdAndTargetIdAndTargetType(Long memberId, Long targetId, TargetType targetType) {
        return likeJpaRepository.findByMemberIdAndTargetIdAndTargetType(memberId, targetId, targetType)
            .map(LikeEntity::toDomain);
    }

    @Override
    public Like save(Like like) {
        LikeEntity entity = LikeEntity.from(like);
        return likeJpaRepository.save(entity).toDomain();
    }

    @Override
    public void delete(Like like) {
        likeJpaRepository.findByMemberIdAndTargetIdAndTargetType(
                like.getMemberId(), like.getTargetId(), like.getTargetType())
            .ifPresent(likeJpaRepository::delete);
    }
}
