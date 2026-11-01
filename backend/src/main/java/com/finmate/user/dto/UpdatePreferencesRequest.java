package com.finmate.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class UpdatePreferencesRequest {

    @NotNull(message = "Theme is required")
    @Pattern(regexp = "^(LIGHT|DARK)$", message = "Theme must be LIGHT or DARK")
    private String theme;

    @Min(value = 5, message = "Page size must be between 5 and 100")
    @Max(value = 100, message = "Page size must be between 5 and 100")
    private int pageSize = 10;

    private boolean emailNotifications = true;

    @Min(value = 50, message = "Alert threshold must be between 50 and 99")
    @Max(value = 99, message = "Alert threshold must be between 50 and 99")
    private int defaultAlertThreshold = 80;

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public boolean isEmailNotifications() {
        return emailNotifications;
    }

    public void setEmailNotifications(boolean emailNotifications) {
        this.emailNotifications = emailNotifications;
    }

    public int getDefaultAlertThreshold() {
        return defaultAlertThreshold;
    }

    public void setDefaultAlertThreshold(int defaultAlertThreshold) {
        this.defaultAlertThreshold = defaultAlertThreshold;
    }
}
