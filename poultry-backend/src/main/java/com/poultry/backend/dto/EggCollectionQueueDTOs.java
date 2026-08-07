package com.poultry.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class EggCollectionQueueDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggQueueItemResponse {
        private Long id;
        private LocalDate queueDate;
        private Long chickenId;
        private String henCode;
        private String henName;
        private String breed;
        private String photoUrl;
        private String pairingCode;
        private LocalDate eggLayingStartDate;
        private Integer currentEggCount;
        private String status; // PENDING, COMPLETED, RESCHEDULED, ESCALATED
        private String noEggReason;
        private Integer healthyEggs;
        private Integer brokenEggs;
        private Integer damagedEggs;
        private String remarks;
        private String assignedWorkerEmail;
        private LocalDateTime rescheduledUntil;
        private Boolean isRescheduledActive;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggQueueSummaryResponse {
        private LocalDate queueDate;
        private Long totalHens;
        private Long pendingCount;
        private Long completedCount;
        private Long rescheduledCount;
        private Long escalatedCount;
        private Double completionRatePercentage;
        private List<EggQueueItemResponse> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmQueueItemRequest {
        @NotNull(message = "Healthy eggs count is required")
        private Integer healthyEggs;
        private Integer brokenEggs;
        private Integer damagedEggs;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NoEggQueueItemRequest {
        @NotNull(message = "Reason is required")
        private String reason;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RescheduleQueueItemRequest {
        @NotNull(message = "Duration in minutes is required")
        private Integer durationMinutes;
    }
}
