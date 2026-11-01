package com.finmate.user.service;

import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.util.DateUtils;
import com.finmate.common.util.MoneyUtils;
import com.finmate.transaction.entity.TransactionType;
import com.finmate.transaction.repository.TransactionRepository;
import com.finmate.user.dto.DeleteAccountRequest;
import com.finmate.user.dto.UpdatePreferencesRequest;
import com.finmate.user.dto.UpdateProfileRequest;
import com.finmate.user.dto.UserPreferencesDto;
import com.finmate.user.dto.UserProfileDto;
import com.finmate.user.entity.User;
import com.finmate.user.entity.UserStatus;
import com.finmate.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       TransactionRepository transactionRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        BigDecimal currentBalance = calculateCurrentBalance(user);
        return UserProfileDto.from(user, currentBalance);
    }

    @Transactional
    public UserProfileDto updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        user.setFullName(request.getFullName().trim());
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getOpeningBalance() != null) {
            user.setOpeningBalance(MoneyUtils.scale(request.getOpeningBalance()));
        }

        User saved = userRepository.save(user);
        BigDecimal currentBalance = calculateCurrentBalance(saved);
        return UserProfileDto.from(saved, currentBalance);
    }

    @Transactional(readOnly = true)
    public UserPreferencesDto getPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return UserPreferencesDto.fromEntity(user);
    }

    @Transactional
    public UserPreferencesDto updatePreferences(Long userId, UpdatePreferencesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        user.setTheme(request.getTheme());
        user.setPageSize(request.getPageSize());
        user.setEmailNotifications(request.isEmailNotifications());
        user.setDefaultAlertThreshold(request.getDefaultAlertThreshold());

        User saved = userRepository.save(user);
        return UserPreferencesDto.fromEntity(saved);
    }

    @Transactional
    public void deleteAccount(Long userId, DeleteAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Password does not match");
        }

        // Anonymize user records and disable account
        user.setStatus(UserStatus.DISABLED);
        user.setFullName("Former User");
        user.setEmail("deleted_" + user.getId() + "@finmate.local");
        userRepository.save(user);
    }

    public BigDecimal calculateCurrentBalance(User user) {
        LocalDate today = DateUtils.today();
        BigDecimal income = transactionRepository.sumAmountByUserAndTypeDatedBeforeOrOn(user.getId(), TransactionType.INCOME, today);
        BigDecimal expense = transactionRepository.sumAmountByUserAndTypeDatedBeforeOrOn(user.getId(), TransactionType.EXPENSE, today);
        return user.getOpeningBalance().add(income).subtract(expense);
    }
}
