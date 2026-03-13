package com.loopers.infrastructure.category;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.category.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "categories")
@SQLDelete(sql = "UPDATE categories SET deleted_at = NOW() WHERE id = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CategoryEntity extends BaseEntity {

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    @Column(name = "path", length = 255)
    private String path;

    @Column(name = "depth")
    private Integer depth;

    public static CategoryEntity from(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.parentId = category.getParentId();
        entity.name = category.getName();
        entity.path = category.getPath();
        entity.depth = category.getDepth();
        return entity;
    }

    public Category toDomain() {
        return new Category(
            getId(),
            parentId,
            name,
            path,
            depth,
            getCreatedAt() != null ? getCreatedAt().toLocalDateTime() : null,
            getUpdatedAt() != null ? getUpdatedAt().toLocalDateTime() : null,
            getDeletedAt() != null ? getDeletedAt().toLocalDateTime() : null
        );
    }

    public void update(String name) {
        this.name = name;
    }

    public void assignPath(String path) {
        this.path = path;
    }
}