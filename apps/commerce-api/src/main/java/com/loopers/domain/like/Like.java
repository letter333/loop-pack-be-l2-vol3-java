package com.loopers.domain.like;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Like {

    private Long id;
    private Long memberId;
    private Long targetId;
    private TargetType targetType;
    private LocalDateTime createdAt;

    public Like(Long memberId, Long targetId, TargetType targetType) {
        this.memberId = memberId;
        this.targetId = targetId;
        this.targetType = targetType;
    }

    public Like(Long id, Long memberId, Long targetId, TargetType targetType, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }
}
