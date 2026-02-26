package com.loopers.infrastructure.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long>, ProductJpaRepositoryCustom {

    Optional<ProductEntity> findByIdAndDeletedAtIsNull(Long id);

    List<ProductEntity> findAllByDeletedAtIsNull();

    List<ProductEntity> findAllByCategoryIdAndDeletedAtIsNull(Long categoryId);

    List<ProductEntity> findAllByBrandIdAndDeletedAtIsNull(Long brandId);

    List<ProductEntity> findAllByCategoryIdInAndDeletedAtIsNull(List<Long> categoryIds);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT DISTINCT p FROM ProductEntity p " +
           "LEFT JOIN FETCH p.options " +
           "LEFT JOIN FETCH p.images " +
           "WHERE p.id = :id")
    Optional<ProductEntity> findByIdWithOptionsAndImages(@Param("id") Long id);

    @Modifying
    @Query("UPDATE ProductEntity p SET p.deletedAt = CURRENT_TIMESTAMP WHERE p.id IN :ids AND p.deletedAt IS NULL")
    void softDeleteAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM ProductEntity p")
    Page<ProductEntity> findAllIncludingDeleted(Pageable pageable);
}