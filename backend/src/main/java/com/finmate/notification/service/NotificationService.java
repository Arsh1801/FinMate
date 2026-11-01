package com.finmate.notification.service;

import com.finmate.common.exception.ApiException;
import com.finmate.notification.channel.NotificationChannel;
import com.finmate.notification.dto.NotificationDto;
import com.finmate.notification.entity.Notification;
import com.finmate.notification.entity.NotificationType;
import com.finmate.notification.repository.NotificationRepository;
import com.finmate.user.entity.User;
import com.finmate.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // Setter injection for optional EmailNotificationChannel as specified in Section 7.1
    private NotificationChannel emailNotificationChannel;

    public NotificationService(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Autowired(required = false)
    public void setEmailNotificationChannel(@Qualifier("emailNotificationChannel") NotificationChannel emailNotificationChannel) {
        this.emailNotificationChannel = emailNotificationChannel;
    }

    @Transactional
    public Optional<Notification> createNotification(Long userId,
                                                     NotificationType type,
                                                     String title,
                                                     String message,
                                                     String refType,
                                                     Long refId,
                                                     String dedupeKey) {
        if (dedupeKey != null && !dedupeKey.isBlank()) {
            if (notificationRepository.findByUserIdAndDedupeKey(userId, dedupeKey.trim()).isPresent()) {
                log.debug("Duplicate notification prevented by dedupe key: {}", dedupeKey);
                return Optional.empty();
            }
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Optional.empty();
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRefType(refType);
        notification.setRefId(refId);
        notification.setDedupeKey(dedupeKey);
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);

        // Async email notification if configured
        if (emailNotificationChannel != null) {
            emailNotificationChannel.send(saved);
        }

        return Optional.of(saved);
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> getUserNotifications(Long userId, boolean unreadOnly, NotificationType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return notificationRepository.findUserNotifications(userId, unreadOnly, type, pageable)
                .map(NotificationDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationDto markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Notification not found");
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return NotificationDto.fromEntity(saved);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ApiException.notFound("Notification not found"));

        if (!notification.getUser().getId().equals(userId)) {
            throw ApiException.notFound("Notification not found");
        }

        notificationRepository.delete(notification);
    }

    @Transactional
    public void purgeOldNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        notificationRepository.purgeOlderThan(cutoff);
    }
}
