package com.finmate.user.service;

import com.finmate.auth.repository.RefreshTokenRepository;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.dto.AdminStatsDto;
import com.finmate.user.dto.AdminUserDto;
import com.finmate.user.entity.Role;
import com.finmate.user.entity.User;
import com.finmate.user.entity.UserStatus;
import com.finmate.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TransactionRepository transactionRepository;

    public AdminService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> getUsers(String search, UserStatus status, int page, int size, String sortField, String sortDir) {
        Sort sort = Sort.by("ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC,
                "createdAt".equalsIgnoreCase(sortField) ? "createdAt" : "id");
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);

        return userRepository.searchUsers(search != null && !search.isBlank() ? search.trim() : null, status, pageable)
                .map(AdminUserDto::fromEntity);
    }

    @Transactional
    public AdminUserDto updateUserStatus(Long adminId, Long targetUserId, UserStatus newStatus) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (adminId.equals(targetUserId)) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Admin cannot modify their own active status");
        }

        if (target.getRole() == Role.ADMIN && newStatus == UserStatus.DISABLED) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Cannot disable the last system administrator");
            }
        }

        target.setStatus(newStatus);
        User saved = userRepository.save(target);

        if (newStatus == UserStatus.DISABLED) {
            // Revoke all sessions immediately
            refreshTokenRepository.revokeAllForUser(target);
        }

        return AdminUserDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AdminStatsDto getPlatformStats(long totalGroupsCount) {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus(UserStatus.ACTIVE);
        long registrationsLast30Days = userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(30));
        long totalTransactions = transactionRepository.count();

        return new AdminStatsDto(totalUsers, activeUsers, registrationsLast30Days, totalGroupsCount, totalTransactions);
    }
}
