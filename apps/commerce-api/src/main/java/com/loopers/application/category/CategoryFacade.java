package com.loopers.application.category;

import com.loopers.domain.category.Category;
import com.loopers.domain.category.CategoryService;
import com.loopers.support.auth.AdminValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CategoryFacade {

    private final CategoryService categoryService;
    private final AdminValidator adminValidator;

    @Transactional(readOnly = true)
    public List<CategoryInfo> getCategoriesHierarchy() {
        List<Category> allCategories = categoryService.getAllActiveCategories();
        return buildHierarchy(allCategories);
    }

    @Transactional
    public CategoryDetailInfo createCategory(String ldap, CategoryCommand.Create command) {
        adminValidator.validate(ldap);
        Category category = categoryService.createCategory(command.name(), command.parentId());
        return CategoryDetailInfo.from(category);
    }

    @Transactional
    public CategoryDetailInfo updateCategory(String ldap, Long categoryId, CategoryCommand.Update command) {
        adminValidator.validate(ldap);
        Category category = categoryService.updateCategory(categoryId, command.name());
        return CategoryDetailInfo.from(category);
    }

    @Transactional
    public void deleteCategory(String ldap, Long categoryId) {
        adminValidator.validate(ldap);
        categoryService.deleteCategory(categoryId);
    }

    private List<CategoryInfo> buildHierarchy(List<Category> categories) {
        Map<Long, List<Category>> childrenMap = categories.stream()
            .filter(c -> c.getParentId() != null)
            .collect(Collectors.groupingBy(Category::getParentId));

        List<Category> rootCategories = categories.stream()
            .filter(Category::isRoot)
            .toList();

        return rootCategories.stream()
            .map(root -> buildCategoryInfo(root, childrenMap))
            .toList();
    }

    private CategoryInfo buildCategoryInfo(Category category, Map<Long, List<Category>> childrenMap) {
        List<Category> children = childrenMap.getOrDefault(category.getId(), List.of());
        List<CategoryInfo> childInfos = children.stream()
            .map(child -> buildCategoryInfo(child, childrenMap))
            .toList();
        return CategoryInfo.withChildren(category, childInfos);
    }
}