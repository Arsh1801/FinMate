package com.finmate.user.dto;

import com.finmate.user.entity.User;

public class UserPreferencesDto {
    private String theme;
    private int pageSize;
    private boolean emailNotifications;
    private int defaultAlertThreshold;

    public UserPreferencesDto() {}

    public UserPreferencesDto(String theme, int pageSize, boolean emailNotifications, int defaultAlertThreshold) {
        this.theme = theme;
        this.pageSize = pageSize;
        this.emailNotifications = emailNotifications;
        this.defaultAlertThreshold = defaultAlertThreshold;
    }

    public static UserPreferencesDto fromEntity(User user) {
        return new UserPreferencesDto(
                user.getTheme(),
                user.getPageSize(),
                user.getEmailNotifications(),
                user.getDefaultAlertThreshold()
        );
    }

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
