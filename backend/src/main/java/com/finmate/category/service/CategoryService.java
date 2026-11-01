package com.finmate.category.service;

import com.finmate.category.dto.CategoryDto;
import com.finmate.category.dto.CreateCategoryRequest;
import com.finmate.category.dto.UpdateCategoryRequest;
import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.repository.CategoryRepository;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository categoryRepository, UserRepository userRepository) {
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAccessibleCategories(Long userId, CategoryType type, boolean includeInactive) {
        return categoryRepository.findAccessibleCategories(userId, type, includeInactive).stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategoriesAdmin(CategoryType type, boolean includeInactive) {
        return categoryRepository.findAllCategoriesAdmin(type, includeInactive).stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryDto createCustomCategory(Long userId, CreateCategoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        String name = request.getName().trim();
        if (categoryRepository.findDuplicate(name, request.getType(), userId).isPresent() ||
            categoryRepository.findDuplicate(name, request.getType(), null).isPresent()) {
            throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A category with name '" + name + "' already exists for " + request.getType());
        }

        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(request.getType());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setIsSystem(false);
        category.setIsActive(true);

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto updateCustomCategory(Long userId, Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw ApiException.forbidden("System categories cannot be modified by users");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Category not found");
        }

        String name = request.getName().trim();
        categoryRepository.findDuplicate(name, category.getType(), userId).ifPresent(dup -> {
            if (!dup.getId().equals(categoryId)) {
                throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A category with name '" + name + "' already exists for " + category.getType());
            }
        });

        category.setName(name);
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto setCustomCategoryStatus(Long userId, Long categoryId, boolean active) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (Boolean.TRUE.equals(category.getIsSystem())) {
            throw ApiException.forbidden("System categories status cannot be modified here");
        }

        if (category.getUser() == null || !category.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Category not found");
        }

        category.setIsActive(active);
        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto createSystemCategory(CreateCategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.findDuplicate(name, request.getType(), null).isPresent()) {
            throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A system category with name '" + name + "' already exists for " + request.getType());
        }

        Category category = new Category();
        category.setUser(null);
        category.setName(name);
        category.setType(request.getType());
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());
        category.setIsSystem(true);
        category.setIsActive(true);

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto updateSystemCategory(Long categoryId, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (!Boolean.TRUE.equals(category.getIsSystem())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Not a system category");
        }

        String name = request.getName().trim();
        categoryRepository.findDuplicate(name, category.getType(), null).ifPresent(dup -> {
            if (!dup.getId().equals(categoryId)) {
                throw ApiException.conflict(ErrorCode.VALIDATION_FAILED, "A system category with name '" + name + "' already exists for " + category.getType());
            }
        });

        category.setName(name);
        category.setColor(request.getColor());
        category.setIcon(request.getIcon());

        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto setSystemCategoryStatus(Long categoryId, boolean active) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        if (!Boolean.TRUE.equals(category.getIsSystem())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Not a system category");
        }

        category.setIsActive(active);
        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Category getCategoryForUserAndType(Long categoryId, Long userId, CategoryType expectedType) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));

        // Must be system category or owned by user
        if (category.getUser() != null && !category.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Category not found");
        }

        // Must match type
        if (category.getType() != expectedType) {
            throw ApiException.businessRule(ErrorCode.CATEGORY_TYPE_MISMATCH,
                    "Category type mismatch: expected " + expectedType + " but category is " + category.getType());
        }

        // Must be active
        if (!Boolean.TRUE.equals(category.getIsActive())) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Category is deactivated");
        }

        return category;
    }
}
