package com.finmate.group.dto;

import java.time.LocalDateTime;

public class GroupActivityDto {
    private String id;
    private String type; // EXPENSE, SETTLEMENT, MEMBERSHIP
    private String title;
    private String description;
    private Long actorId;
    private String actorName;
    private String amount;
    private LocalDateTime timestamp;

    public GroupActivityDto() {}

    public GroupActivityDto(String id, String type, String title, String description, Long actorId, String actorName, String amount, LocalDateTime timestamp) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.description = description;
        this.actorId = actorId;
        this.actorName = actorName;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
