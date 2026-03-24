package com.loopers.infrastructure.actionlog;

import com.loopers.domain.actionlog.UserActionLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Entity
@Table(
    name = "user_action_log",
    indexes = {
        @Index(name = "idx_user_action_log_member", columnList = "member_id"),
        @Index(name = "idx_user_action_log_target", columnList = "target_id, target_type")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "action_type", nullable = false, length = 30)
    private String actionType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_type", nullable = false, length = 30)
    private String targetType;

    @Column(name = "metadata", length = 500)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @PrePersist
    private void prePersist() {
        this.createdAt = ZonedDateTime.now();
    }

    public static UserActionLogEntity from(UserActionLog log) {
        UserActionLogEntity entity = new UserActionLogEntity();
        entity.memberId = log.getMemberId();
        entity.actionType = log.getActionType();
        entity.targetId = log.getTargetId();
        entity.targetType = log.getTargetType();
        entity.metadata = log.getMetadata();
        return entity;
    }

    public UserActionLog toDomain() {
        return new UserActionLog(
            id,
            memberId,
            actionType,
            targetId,
            targetType,
            metadata,
            createdAt != null ? createdAt.toLocalDateTime() : null
        );
    }
}
