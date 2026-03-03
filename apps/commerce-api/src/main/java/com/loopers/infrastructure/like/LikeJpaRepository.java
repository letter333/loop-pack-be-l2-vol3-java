package com.loopers.infrastructure.like;

import com.loopers.domain.like.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LikeJpaRepository extends JpaRepository<LikeEntity, Long> {

    Optional<LikeEntity> findByMemberIdAndTargetIdAndTargetType(Long memberId, Long targetId, TargetType targetType);
}
