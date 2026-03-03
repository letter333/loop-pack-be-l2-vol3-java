package com.loopers.domain.category;

import com.loopers.domain.product.ProductService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Category getCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "카테고리를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Category getActiveCategory(Long categoryId) {
        Category category = getCategory(categoryId);
        if (category.isDeleted()) {
            throw new CoreException(ErrorType.NOT_FOUND, "삭제된 카테고리입니다.");
        }
        return category;
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<Category> getAllActiveCategories() {
        return categoryRepository.findAllActive();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Category createCategory(String name, Long parentId) {
        if (parentId == null) {
            Category category = new Category(name);
            Category saved = categoryRepository.save(category);
            saved.assignPath(saved.getId());
            return categoryRepository.save(saved);
        }

        Category parent = getActiveCategory(parentId);
        Category category = new Category(name, parentId, parent.getPath(), parent.getDepth());
        Category saved = categoryRepository.save(category);
        saved.assignChildPath(parent.getPath(), saved.getId());
        return categoryRepository.save(saved);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Category updateCategory(Long categoryId, String name) {
        Category category = getCategory(categoryId);
        category.update(name);
        return categoryRepository.save(category);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void deleteCategory(Long categoryId) {
        Category category = getCategory(categoryId);

        // 삭제할 카테고리 ID 수집 (자신 + 하위)
        List<Long> categoryIdsToDelete = new ArrayList<>();
        categoryIdsToDelete.add(categoryId);

        // 하위 카테고리도 함께 삭제 (CAT-012)
        List<Category> children = categoryRepository.findAllActiveChildrenByPath(category.getPath() + "/");
        categoryIdsToDelete.addAll(children.stream().map(Category::getId).toList());

        // 연관 상품 삭제
        productService.deleteProductsByCategoryIds(categoryIdsToDelete);

        for (Category child : children) {
            categoryRepository.delete(child.getId());
        }

        categoryRepository.delete(categoryId);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Category validateCategory(Long categoryId) {
        return getActiveCategory(categoryId);
    }
}