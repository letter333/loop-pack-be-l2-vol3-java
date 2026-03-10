package com.loopers.infrastructure.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BrandJpaRepository extends JpaRepository<BrandEntity, Long> {

    List<BrandEntity> findByDeletedAtIsNull();

    Page<BrandEntity> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT b FROM BrandEntity b WHERE b.id IN :ids AND b.deletedAt IS NULL")
    List<BrandEntity> findAllActiveByIdIn(@Param("ids") List<Long> ids);

    @Modifying
    @Query(value = "UPDATE brands SET like_count = like_count + 1 WHERE id = :id", nativeQuery = true)
    void increaseLikeCount(@Param("id") Long id);

    @Modifying
    @Query(value = "UPDATE brands SET like_count = like_count - 1 WHERE id = :id AND like_count > 0", nativeQuery = true)
    void decreaseLikeCount(@Param("id") Long id);

    @Query(value = "SELECT like_count FROM brands WHERE id = :id", nativeQuery = true)
    Long findLikeCountById(@Param("id") Long id);
}