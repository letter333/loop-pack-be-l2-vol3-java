package com.loopers.infrastructure.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

@Entity
@Table(name = "brands")
@SQLDelete(sql = "UPDATE brands SET deleted_at = NOW() WHERE id = ? AND version = ?")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BrandEntity extends BaseEntity {

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_image_url", length = 512)
    private String logoImageUrl;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public static BrandEntity from(Brand brand) {
        BrandEntity entity = new BrandEntity();
        entity.name = brand.getName();
        entity.description = brand.getDescription();
        entity.logoImageUrl = brand.getLogoImageUrl();
        entity.likeCount = brand.getLikeCount() != null ? brand.getLikeCount() : 0L;
        return entity;
    }

    public Brand toDomain() {
        return new Brand(
            getId(),
            name,
            description,
            logoImageUrl,
            likeCount,
            version,
            getCreatedAt() != null ? getCreatedAt().toLocalDateTime() : null,
            getUpdatedAt() != null ? getUpdatedAt().toLocalDateTime() : null,
            getDeletedAt() != null ? getDeletedAt().toLocalDateTime() : null
        );
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void updateLikeCount(Long likeCount) {
        this.likeCount = likeCount != null ? likeCount : 0L;
    }

    public void update(String name, String description, String logoImageUrl) {
        this.name = name;
        this.description = description;
        this.logoImageUrl = logoImageUrl;
    }
}