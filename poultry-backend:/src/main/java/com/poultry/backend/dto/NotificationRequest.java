package com.poultry.backend.dto;

import com.poultry.backend.entity.NotificationType;
import com.poultry.backend.entity.RecipientRole;
import com.poultry.backend.entity.Severity;
import com.poultry.backend.entity.SourceModule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification payload for creating a notification record")
public class NotificationRequest {

    @NotBlank(message = "Title is required")
    @Schema(description = "Title of the alert", example = "Low Feed Inventory")
    private String title;

    @NotBlank(message = "Message is required")
    @Schema(description = "Notification message text content", example = "Feed item standard broiler mash stock level is below critical threshhold.")
    private String message;

    @NotNull(message = "Notification type is required")
    @Schema(description = "Category of notification", example = "FEED")
    private NotificationType notificationType;

    @NotNull(message = "Severity scale is required")
    @Schema(description = "Notification severity tier scale", example = "WARNING")
    private Severity severity;

    @NotNull(message = "Source module is required")
    @Schema(description = "The originating module of the alert", example = "FEED")
    private SourceModule sourceModule;

    @Schema(description = "Optionally references category name of source entity", example = "FEED_ITEM")
    private String referenceType;

    @Schema(description = "Optionally references secondary database ID of source entity", example = "10")
    private Long referenceId;

    @NotNull(message = "Recipient role is required")
    @Schema(description = "User role level authorized to view this alert", example = "MANAGER")
    private RecipientRole recipientRole;
}
