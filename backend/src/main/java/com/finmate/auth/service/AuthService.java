package com.finmate.auth.service;

import com.finmate.auth.dto.*;
import com.finmate.auth.entity.PasswordResetToken;
import com.finmate.auth.entity.RefreshToken;
import com.finmate.auth.repository.PasswordResetTokenRepository;
import com.finmate.auth.repository.RefreshTokenRepository;
import com.finmate.common.exception.ApiException;
import com.finmate.common.exception.ErrorCode;
import com.finmate.common.security.JwtTokenProvider;
import com.finmate.user.entity.Role;
import com.finmate.user.entity.User;
import com.finmate.user.entity.UserStatus;
import com.finmate.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final Optional<JavaMailSender> mailSender;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       Optional<JavaMailSender> mailSender) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.mailSender = mailSender;
    }

    @Transactional
    public UserSummaryDto register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Passwords do not match");
        }

        String email = request.getEmail().trim().toLowerCase();
        String localPart = email.split("@")[0].toLowerCase();
        if (request.getPassword().toLowerCase().contains(localPart)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Password must not contain the email's local part");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict(ErrorCode.EMAIL_ALREADY_REGISTERED, "Email is already registered");
        }

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.ACTIVE);
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getOpeningBalance() != null && request.getOpeningBalance().compareTo(BigDecimal.ZERO) >= 0) {
            user.setOpeningBalance(request.getOpeningBalance().setScale(2, java.math.RoundingMode.HALF_UP));
        } else {
            user.setOpeningBalance(BigDecimal.ZERO.setScale(2));
        }

        User saved = userRepository.save(user);
        log.info("New user registered successfully: id={}, email={}", saved.getId(), saved.getEmail());
        return UserSummaryDto.fromEntity(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isEmpty()) {
            throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        User user = userOpt.get();

        // Check if account is locked
        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(now)) {
                long minutesLeft = Duration.between(now, user.getLockedUntil()).toMinutes() + 1;
                throw ApiException.locked("Account is temporarily locked. Try again in " + minutesLeft + " minutes.");
            } else {
                // Unlock account
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
            }
        }

        // Check if account is disabled
        if (user.getStatus() == UserStatus.DISABLED) {
            throw ApiException.forbidden("Account is disabled. Please contact the administrator.");
        }

        // Check password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(now.plusMinutes(15));
                userRepository.save(user);
                throw ApiException.locked("Account is temporarily locked due to 5 failed attempts. Try again in 15 minutes.");
            }
            userRepository.save(user);
            throw ApiException.unauthorized(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        // Login successful: reset failed attempts
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        // Generate tokens
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole().name());
        String rawRefreshToken = tokenProvider.generateRandomToken();
        String hashedRefresh = tokenProvider.sha256Hex(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashedRefresh);
        refreshToken.setExpiresAt(now.plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000));
        refreshToken.setRevoked(false);
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                tokenProvider.getAccessTokenExpirationSeconds(),
                rawRefreshToken,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw ApiException.unauthorized(ErrorCode.UNAUTHENTICATED, "Refresh token is required");
        }

        String hash = tokenProvider.sha256Hex(rawRefreshToken.trim());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.unauthorized(ErrorCode.UNAUTHENTICATED, "Invalid refresh token"));

        // Reuse detection: if token was already revoked, revoke ALL user refresh tokens (family revocation)
        if (Boolean.TRUE.equals(token.getRevoked())) {
            log.warn("Revoked refresh token reused for user {}! Revoking all sessions.", token.getUser().getId());
            refreshTokenRepository.revokeAllForUser(token.getUser());
            throw ApiException.unauthorized(ErrorCode.UNAUTHENTICATED, "Refresh token was already revoked. All active sessions invalidated.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (token.getExpiresAt().isBefore(now)) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw ApiException.unauthorized(ErrorCode.UNAUTHENTICATED, "Refresh token has expired. Please log in again.");
        }

        User user = token.getUser();
        if (user.getStatus() == UserStatus.DISABLED) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw ApiException.forbidden("Account is disabled");
        }

        // Rotate token
        String newRaw = tokenProvider.generateRandomToken();
        String newHash = tokenProvider.sha256Hex(newRaw);

        RefreshToken newToken = new RefreshToken();
        newToken.setUser(user);
        newToken.setTokenHash(newHash);
        newToken.setExpiresAt(now.plusSeconds(tokenProvider.getRefreshTokenExpirationMs() / 1000));
        newToken.setRevoked(false);
        RefreshToken savedNewToken = refreshTokenRepository.save(newToken);

        token.setRevoked(true);
        token.setReplacedById(savedNewToken.getId());
        refreshTokenRepository.save(token);

        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getRole().name());

        return new AuthResponse(
                newAccessToken,
                tokenProvider.getAccessTokenExpirationSeconds(),
                newRaw,
                UserSummaryDto.fromEntity(user)
        );
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            String hash = tokenProvider.sha256Hex(rawRefreshToken.trim());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String rawToken = tokenProvider.generateRandomToken();
            String hash = tokenProvider.sha256Hex(rawToken);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(hash);
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            passwordResetTokenRepository.save(resetToken);

            // Send email asynchronously / logged
            sendPasswordResetEmail(user.getEmail(), rawToken);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = tokenProvider.sha256Hex(request.getToken().trim());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Invalid or expired reset token"));

        if (resetToken.getUsedAt() != null) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Reset token has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Reset token has expired");
        }

        User user = resetToken.getUser();
        String localPart = user.getEmail().split("@")[0].toLowerCase();
        if (request.getNewPassword().toLowerCase().contains(localPart)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Password must not contain the email's local part");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);

        // Revoke all refresh tokens for this user
        refreshTokenRepository.revokeAllForUser(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw ApiException.businessRule(ErrorCode.VALIDATION_FAILED, "Current password does not match");
        }

        String localPart = user.getEmail().split("@")[0].toLowerCase();
        if (request.getNewPassword().toLowerCase().contains(localPart)) {
            throw ApiException.badRequest(ErrorCode.VALIDATION_FAILED, "Password must not contain the email's local part");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revoke other sessions
        refreshTokenRepository.revokeAllForUser(user);
    }

    private void sendPasswordResetEmail(String email, String rawToken) {
        String resetLink = "http://localhost:5173/reset-password?token=" + rawToken;
        log.info("------------------------------------------------------------------");
        log.info("PASSWORD RESET EMAIL DISPATCHED to {}: link={}", email, resetLink);
        log.info("------------------------------------------------------------------");

        mailSender.ifPresent(sender -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(email);
                message.setSubject("FinMate - Password Reset Request");
                message.setText("Click the following link to reset your FinMate password:\n\n" +
                        resetLink + "\n\nThis link will expire in 30 minutes.\nIf you did not request this, please ignore this email.");
                sender.send(message);
            } catch (Exception ex) {
                log.warn("Failed to deliver SMTP reset email (logged above): {}", ex.getMessage());
            }
        });
    }
}
