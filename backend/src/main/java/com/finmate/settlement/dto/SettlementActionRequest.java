package com.finmate.settlement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SettlementActionRequest {

    @NotBlank(message = "Action is required")
    @Pattern(regexp = "^(CONFIRM|REJECT|REVERSE)$", message = "Action must be CONFIRM, REJECT, or REVERSE")
    private String action;

    @NotNull(message = "Version is required for optimistic locking check")
    private Integer version;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
