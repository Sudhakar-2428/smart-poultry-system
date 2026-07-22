package com.poultry.backend.dto;

import com.poultry.backend.entity.NotificationType;
import com.poultry.backend.entity.RecipientRole;
import com.poultry.backend.entity.Severity;
import com.poultry.backend.entity.SourceModule;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification response payload details")
public class NotificationResponse {

    @Schema(description = "Unique ID of the notification", example = "1")
    private Long id;

    @Schema(description = "Short notification title/header", example = "Vaccination Overdue")
    private String title;

    @Schema(description = "Detailed notification description/content", example = "Vaccination campaign 'Newcastle Disease' is overdue for chicken CHK-F-01")
    private String message;

    @Schema(description = "Broad category type of this notification", example = "HEALTH")
    private NotificationType notificationType;

    @Schema(description = "Severity tier ranking", example = "WARNING")
    private Severity severity;

    @Schema(description = "System module origin", example = "HEALTH")
    private SourceModule sourceModule;

    @Schema(description = "Optionally references category name of the source entity", example = "CHICKEN")
    private String referenceType;

    @Schema(description = "Optionally references secondary database ID of source entity", example = "105")
    private Long referenceId;

    @Schema(description = "Recipient user role intended access level", example = "VETERINARIAN")
    private RecipientRole recipientRole;

    @Schema(description = "Indicates if user has marked this alert read", example = "false")
    private boolean isRead;

    @Schema(description = "Timestamp when user marked read", example = "2026-07-19T21:00:00")
    private LocalDateTime readAt;

    @Schema(description = "Indicates if user has archived this record", example = "false")
    private boolean isArchived;

    @Schema(description = "Timestamp when user archived", example = "2026-07-19T21:00:00")
    private LocalDateTime archivedAt;

    @Schema(description = "Notification origin creation timestamp", example = "2026-07-19T20:00:00")
    private LocalDateTime createdAt;
}
