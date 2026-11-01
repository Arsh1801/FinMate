package com.finmate.user.controller;

import com.finmate.common.util.SecurityUtils;
import com.finmate.group.repository.ExpenseGroupRepository;
import com.finmate.user.dto.AdminStatsDto;
import com.finmate.user.dto.AdminUserDto;
import com.finmate.user.dto.UserStatusUpdateRequest;
import com.finmate.user.entity.UserStatus;
import com.finmate.user.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Console", description = "Endpoints for platform administration, user management, and aggregate statistics")
public class AdminController {

    private final AdminService adminService;
    private final ExpenseGroupRepository expenseGroupRepository;

    public AdminController(AdminService adminService, ExpenseGroupRepository expenseGroupRepository) {
        this.adminService = adminService;
        this.expenseGroupRepository = expenseGroupRepository;
    }

    @GetMapping("/users")
    @Operation(summary = "List and search users (no personal financial data)")
    public ResponseEntity<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return ResponseEntity.ok(adminService.getUsers(search, status, page, size, sort, dir));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Enable or disable user account (revokes sessions on disable)")
    public ResponseEntity<AdminUserDto> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UserStatusUpdateRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(adminService.updateUserStatus(adminId, userId, request.getStatus()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-level aggregate counts")
    public ResponseEntity<AdminStatsDto> getPlatformStats() {
        long totalGroups = expenseGroupRepository.count();
        return ResponseEntity.ok(adminService.getPlatformStats(totalGroups));
    }
}
