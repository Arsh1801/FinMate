package com.finmate.category.dto;

import jakarta.validation.constraints.NotNull;

public class CategoryStatusRequest {

    @NotNull(message = "Active flag is required")
    private Boolean active;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
