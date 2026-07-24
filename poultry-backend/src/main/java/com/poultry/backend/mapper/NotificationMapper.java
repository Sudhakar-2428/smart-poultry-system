package com.poultry.backend.mapper;

import com.poultry.backend.dto.NotificationResponse;
import com.poultry.backend.entity.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .severity(notification.getSeverity())
                .sourceModule(notification.getSourceModule())
                .referenceType(notification.getReferenceType())
                .referenceId(notification.getReferenceId())
                .recipientRole(notification.getRecipientRole())
                .isRead(notification.isRead())
                .readAt(notification.getReadAt())
                .isArchived(notification.isArchived())
                .archivedAt(notification.getArchivedAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
