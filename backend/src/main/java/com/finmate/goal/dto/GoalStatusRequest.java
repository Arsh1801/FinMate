package com.finmate.goal.dto;

import com.finmate.goal.entity.GoalStatus;
import jakarta.validation.constraints.NotNull;

public class GoalStatusRequest {

    @NotNull(message = "Status is required")
    private GoalStatus status;

    public GoalStatus getStatus() {
        return status;
    }

    public void setStatus(GoalStatus status) {
        this.status = status;
    }
}
