package com.poultry.backend.service;

import com.poultry.backend.dto.NotificationResponse;
import com.poultry.backend.dto.NotificationRequest;
import com.poultry.backend.entity.NotificationType;
import com.poultry.backend.entity.RecipientRole;
import com.poultry.backend.entity.Severity;
import com.poultry.backend.entity.SourceModule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long id);

    NotificationResponse markAsRead(Long id);

    NotificationResponse archiveNotification(Long id);

    long getUnreadCount();

    Page<NotificationResponse> searchNotifications(
            NotificationType type,
            Severity severity,
            SourceModule sourceModule,
            RecipientRole recipientRole,
            Boolean isRead,
            Boolean isArchived,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable
    );

    int bulkRead();

    // Dashboard Integrations
    List<NotificationResponse> getRecentNotifications(int limit);

    List<NotificationResponse> getCriticalNotifications();

    List<NotificationResponse> getUnreadNotifications();

    Map<String, Long> getNotificationCountBySeverity();

    Map<String, Long> getNotificationCountByModule();
}
