package com.poultry.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EggNotificationDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggNotificationResponse {
        private Long id;
        private Long chickenId;
        private String henCode;
        private String henName;
        private String breed;
        private String photoUrl;
        private Integer henAgeInWeeks;
        private String currentBatchCode;
        private Integer currentEggCount;
        private LocalDate notificationDate;
        private String status; // PENDING, COMPLETED, NO_EGG, ESCALATED, OVERDUE, DISMISSED
        private String noEggReason;
        private Integer healthyEggs;
        private Integer brokenEggs;
        private Integer damagedEggs;
        private String remarks;
        private LocalDateTime rescheduledUntil;
        private Boolean isRescheduledActive;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmEggCollectionRequest {
        @NotNull(message = "Healthy eggs count is required")
        @Min(value = 0, message = "Healthy eggs count cannot be negative")
        private Integer healthyEggs;

        @Min(value = 0, message = "Broken eggs count cannot be negative")
        private Integer brokenEggs;

        @Min(value = 0, message = "Damaged eggs count cannot be negative")
        private Integer damagedEggs;

        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoEggReasonRequest {
        @NotBlank(message = "Reason is required")
        private String reason; // No Egg Today, Brooding, Sick, Stress, Low Feed Intake, Other

        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RescheduleNotificationRequest {
        @NotNull(message = "Duration in minutes is required")
        @Min(value = 15, message = "Duration must be at least 15 minutes")
        private Integer durationMinutes; // 30, 60, 120, 180, 240, 300
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggNotificationReportDTO {
        private String reportTitle;
        private Long totalNotifications;
        private Long pendingCount;
        private Long completedCount;
        private Long noEggCount;
        private Long escalatedCount;
        private List<EggNotificationResponse> notifications;
    }
}
