package com.loopers.domain.category;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class Category {

    private Long id;
    private Long parentId;
    private String name;
    private String path;
    private Integer depth;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Category(String name) {
        validateName(name);
        this.name = name;
        this.parentId = null;
        this.depth = 0;
    }

    public Category(String name, Long parentId, String parentPath, Integer parentDepth) {
        validateName(name);
        this.name = name;
        this.parentId = parentId;
        this.depth = parentDepth + 1;
    }

    public Category(Long id, Long parentId, String name, String path, Integer depth,
                    LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        this.path = path;
        this.depth = depth;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public boolean isRoot() {
        return parentId == null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void update(String name) {
        validateName(name);
        this.name = name;
    }

    public void assignPath(Long savedId) {
        if (isRoot()) {
            this.path = String.valueOf(savedId);
        }
    }

    public void assignChildPath(String parentPath, Long savedId) {
        this.path = parentPath + "/" + savedId;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카테고리명은 필수입니다.");
        }
    }
}