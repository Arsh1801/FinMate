package com.finmate.category.controller;

import com.finmate.category.dto.CategoryDto;
import com.finmate.category.dto.CategoryStatusRequest;
import com.finmate.category.dto.CreateCategoryRequest;
import com.finmate.category.dto.UpdateCategoryRequest;
import com.finmate.category.entity.CategoryType;
import com.finmate.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Categories", description = "Endpoints for administrator management of system categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List all categories for admin review")
    public ResponseEntity<List<CategoryDto>> getAllCategories(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(defaultValue = "true") boolean includeInactive) {
        return ResponseEntity.ok(categoryService.getAllCategoriesAdmin(type, includeInactive));
    }

    @PostMapping
    @Operation(summary = "Create a default system category")
    public ResponseEntity<CategoryDto> createSystemCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto created = categoryService.createSystemCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a default system category")
    public ResponseEntity<CategoryDto> updateSystemCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateSystemCategory(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a system category")
    public ResponseEntity<CategoryDto> setSystemCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryStatusRequest request) {
        return ResponseEntity.ok(categoryService.setSystemCategoryStatus(id, request.getActive()));
    }
}
