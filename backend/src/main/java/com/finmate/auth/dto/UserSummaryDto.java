package com.finmate.auth.dto;

import com.finmate.user.entity.Role;
import com.finmate.user.entity.User;

public class UserSummaryDto {
    private Long id;
    private String fullName;
    private String email;
    private Role role;

    public UserSummaryDto() {}

    public UserSummaryDto(Long id, String fullName, String email, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
    }

    public static UserSummaryDto fromEntity(User user) {
        return new UserSummaryDto(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
