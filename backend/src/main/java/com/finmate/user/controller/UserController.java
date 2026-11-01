package com.finmate.user.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.user.dto.*;
import com.finmate.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile & Preferences", description = "Endpoints for managing authenticated user profile and settings")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile with opening and calculated current balance")
    public ResponseEntity<UserProfileDto> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    @Operation(summary = "Update user profile (full name, phone, opening balance)")
    public ResponseEntity<UserProfileDto> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @GetMapping("/me/preferences")
    @Operation(summary = "Read user preferences (theme, page size, email notifications, budget alert threshold)")
    public ResponseEntity<UserPreferencesDto> getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.getPreferences(userId));
    }

    @PutMapping("/me/preferences")
    @Operation(summary = "Update user preferences")
    public ResponseEntity<UserPreferencesDto> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(userService.updatePreferences(userId, request));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete account when all shared group balances are zero")
    public ResponseEntity<Void> deleteAccount(@Valid @RequestBody DeleteAccountRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        userService.deleteAccount(userId, request);
        return ResponseEntity.noContent().build();
    }
}
