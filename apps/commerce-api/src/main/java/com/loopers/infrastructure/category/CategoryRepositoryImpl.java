package com.loopers.infrastructure.category;

import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CategoryRepositoryImpl implements CategoryRepository {

    private final CategoryJpaRepository categoryJpaRepository;

    @Override
    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id)
            .map(CategoryEntity::toDomain);
    }

    @Override
    public List<Category> findAllActive() {
        return categoryJpaRepository.findByDeletedAtIsNull().stream()
            .map(CategoryEntity::toDomain)
            .toList();
    }

    @Override
    public List<Category> findAllActiveChildrenByPath(String pathPrefix) {
        return categoryJpaRepository.findByPathStartingWithAndDeletedAtIsNull(pathPrefix).stream()
            .map(CategoryEntity::toDomain)
            .toList();
    }

    @Override
    public Category save(Category category) {
        CategoryEntity entity;
        if (category.getId() != null) {
            entity = categoryJpaRepository.findById(category.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
            entity.update(category.getName());
            if (category.getPath() != null && entity.getPath() == null) {
                entity.assignPath(category.getPath());
            }
        } else {
            entity = CategoryEntity.from(category);
        }
        CategoryEntity saved = categoryJpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public void delete(Long id) {
        categoryJpaRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return categoryJpaRepository.existsById(id);
    }
}