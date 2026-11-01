package com.finmate.user.dto;

import com.finmate.user.entity.Role;
import com.finmate.user.entity.User;

import java.math.BigDecimal;

public class UserProfileDto {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private String currencyCode;
    private String openingBalance;
    private String currentBalance;

    public UserProfileDto() {}

    public UserProfileDto(Long id, String fullName, String email, String phone, Role role,
                          String currencyCode, BigDecimal openingBalance, BigDecimal currentBalance) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.currencyCode = currencyCode;
        this.openingBalance = openingBalance != null ? openingBalance.toPlainString() : "0.00";
        this.currentBalance = currentBalance != null ? currentBalance.toPlainString() : "0.00";
    }

    public static UserProfileDto from(User user, BigDecimal currentBalance) {
        return new UserProfileDto(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCurrencyCode(),
                user.getOpeningBalance(),
                currentBalance
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(String openingBalance) {
        this.openingBalance = openingBalance;
    }

    public String getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(String currentBalance) {
        this.currentBalance = currentBalance;
    }
}
