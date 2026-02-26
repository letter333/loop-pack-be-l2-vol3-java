package com.loopers.domain.category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(Long id);

    List<Category> findAllActive();

    List<Category> findAllActiveByParentId(Long parentId);

    List<Category> findAllActiveChildrenByPath(String pathPrefix);

    Category save(Category category);

    void delete(Long id);

    boolean existsById(Long id);
}
