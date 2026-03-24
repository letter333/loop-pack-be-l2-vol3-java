package com.loopers.domain.actionlog;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserActionLog {

    private Long id;
    private Long memberId;
    private String actionType;
    private Long targetId;
    private String targetType;
    private String metadata;
    private LocalDateTime createdAt;

    public UserActionLog(Long memberId, String actionType, Long targetId, String targetType, String metadata) {
        this.memberId = memberId;
        this.actionType = actionType;
        this.targetId = targetId;
        this.targetType = targetType;
        this.metadata = metadata;
        this.createdAt = LocalDateTime.now();
    }

    public UserActionLog(Long id, Long memberId, String actionType, Long targetId, String targetType,
                         String metadata, LocalDateTime createdAt) {
        this.id = id;
        this.memberId = memberId;
        this.actionType = actionType;
        this.targetId = targetId;
        this.targetType = targetType;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
}
