package com.finmate.dashboard.dto;

import java.math.BigDecimal;

public class CategoryDistributionDto {
    private Long categoryId;
    private String name;
    private String color;
    private String amount;
    private BigDecimal percent;

    public CategoryDistributionDto() {}

    public CategoryDistributionDto(Long categoryId, String name, String color, String amount, BigDecimal percent) {
        this.categoryId = categoryId;
        this.name = name;
        this.color = color;
        this.amount = amount;
        this.percent = percent;
    }

    public CategoryDistributionDto(String name, BigDecimal amount, BigDecimal percent, String color) {
        this.name = name;
        this.color = color;
        this.amount = amount != null ? amount.toPlainString() : "0.00";
        this.percent = percent;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public BigDecimal getPercent() {
        return percent;
    }

    public void setPercent(BigDecimal percent) {
        this.percent = percent;
    }
}
