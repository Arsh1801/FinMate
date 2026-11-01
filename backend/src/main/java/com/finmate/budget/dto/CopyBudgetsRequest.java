package com.finmate.budget.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CopyBudgetsRequest {

    @NotNull(message = "fromYear is required")
    private Integer fromYear;

    @NotNull(message = "fromMonth is required")
    @Min(1) @Max(12)
    private Integer fromMonth;

    @NotNull(message = "toYear is required")
    private Integer toYear;

    @NotNull(message = "toMonth is required")
    @Min(1) @Max(12)
    private Integer toMonth;

    public Integer getFromYear() {
        return fromYear;
    }

    public void setFromYear(Integer fromYear) {
        this.fromYear = fromYear;
    }

    public Integer getFromMonth() {
        return fromMonth;
    }

    public void setFromMonth(Integer fromMonth) {
        this.fromMonth = fromMonth;
    }

    public Integer getToYear() {
        return toYear;
    }

    public void setToYear(Integer toYear) {
        this.toYear = toYear;
    }

    public Integer getToMonth() {
        return toMonth;
    }

    public void setToMonth(Integer toMonth) {
        this.toMonth = toMonth;
    }
}
