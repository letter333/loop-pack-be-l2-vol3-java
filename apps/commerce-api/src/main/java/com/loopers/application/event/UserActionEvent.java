package com.loopers.application.event;

public record UserActionEvent(
    Long memberId,
    ActionType actionType,
    Long targetId,
    String targetType,
    String metadata
) {
}
