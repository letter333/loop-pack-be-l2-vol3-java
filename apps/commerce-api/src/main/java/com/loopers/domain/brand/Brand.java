package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Brand {

    private Long id;
    private String name;
    private String description;
    private String logoImageUrl;
    private Long likeCount;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Brand(String name, String description, String logoImageUrl) {
        validateName(name);
        this.name = name;
        this.description = description;
        this.logoImageUrl = logoImageUrl;
        this.likeCount = 0L;
    }

    public Brand(Long id, String name, String description, String logoImageUrl, Long likeCount,
                 Long version, LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.logoImageUrl = logoImageUrl;
        this.likeCount = likeCount != null ? likeCount : 0L;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public void update(String name, String description, String logoImageUrl) {
        validateName(name);
        this.name = name;
        this.description = description;
        this.logoImageUrl = logoImageUrl;
    }

    public void delete() {
        if (this.deletedAt == null) {
            this.deletedAt = LocalDateTime.now();
        }
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수입니다.");
        }
    }
}