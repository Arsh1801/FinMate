package com.finmate.notification.repository;

import com.finmate.notification.entity.Notification;
import com.finmate.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND " +
           "(:unreadOnly = false OR n.isRead = false) AND " +
           "(:type IS NULL OR n.type = :type) ORDER BY n.createdAt DESC")
    Page<Notification> findUserNotifications(@Param("userId") Long userId,
                                             @Param("unreadOnly") boolean unreadOnly,
                                             @Param("type") NotificationType type,
                                             Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByUserIdAndDedupeKey(Long userId, String dedupeKey);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    void markAllReadForUser(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :cutoff")
    void purgeOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
