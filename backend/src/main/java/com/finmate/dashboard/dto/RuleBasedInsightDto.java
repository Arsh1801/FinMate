package com.finmate.dashboard.dto;

public class RuleBasedInsightDto {
    private String type; // INFO, WARNING, SUCCESS, TIP
    private String title;
    private String message;
    private String category;

    public RuleBasedInsightDto() {}

    public RuleBasedInsightDto(String type, String title, String message, String category) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.category = category;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
