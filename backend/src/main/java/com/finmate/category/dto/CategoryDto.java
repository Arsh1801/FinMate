package com.finmate.category.dto;

import com.finmate.category.entity.Category;
import com.finmate.category.entity.CategoryType;

public class CategoryDto {
    private Long id;
    private String name;
    private CategoryType type;
    private String color;
    private String icon;
    private boolean isSystem;
    private boolean isActive;

    public CategoryDto() {}

    public CategoryDto(Long id, String name, CategoryType type, String color, String icon, boolean isSystem, boolean isActive) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.color = color;
        this.icon = icon;
        this.isSystem = isSystem;
        this.isActive = isActive;
    }

    public static CategoryDto fromEntity(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getType(),
                category.getColor(),
                category.getIcon(),
                Boolean.TRUE.equals(category.getIsSystem()),
                Boolean.TRUE.equals(category.getIsActive())
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryType getType() {
        return type;
    }

    public void setType(CategoryType type) {
        this.type = type;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public void setSystem(boolean system) {
        isSystem = system;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}
