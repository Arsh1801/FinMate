package com.finmate.category.controller;

import com.finmate.category.dto.CategoryDto;
import com.finmate.category.dto.CategoryStatusRequest;
import com.finmate.category.dto.CreateCategoryRequest;
import com.finmate.category.dto.UpdateCategoryRequest;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.service.CategoryService;
import com.finmate.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Endpoints for viewing and managing transaction categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List accessible categories (system categories plus user's custom categories)")
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(defaultValue = "false") boolean includeInactive) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.getAccessibleCategories(userId, type, includeInactive));
    }

    @PostMapping
    @Operation(summary = "Create a custom category")
    public ResponseEntity<CategoryDto> createCustomCategory(@Valid @RequestBody CreateCategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        CategoryDto created = categoryService.createCustomCategory(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename or update a custom category")
    public ResponseEntity<CategoryDto> updateCustomCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.updateCustomCategory(userId, id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a custom category")
    public ResponseEntity<CategoryDto> setCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(categoryService.setCustomCategoryStatus(userId, id, request.getActive()));
    }
}
