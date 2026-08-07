package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class WorkerProductivityDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerProductivitySummary {
        private LocalDate date;
        private Long totalScheduledHens;
        private Long completedHens;
        private Long pendingHens;
        private Long rescheduledHens;
        private Long escalatedHens;
        private Double overallCompletionRatePercentage;
        private String bestPerformingWorker;
        private String slowestResponseTime;
        private List<WorkerPerformanceDTO> workerLeaderboard;
        private List<LiveActivityFeedItem> liveActivityFeed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkerPerformanceDTO {
        private String workerName;
        private String workerEmail;
        private String avatarUrl;
        private Long assignedHens;
        private Long completedHens;
        private Long pendingHens;
        private Long rescheduledHens;
        private Long escalatedHens;
        private Double completionRatePercentage;
        private String avgResponseTime; // e.g. "8 mins"
        private LocalDateTime lastActivityTime;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiveActivityFeedItem {
        private Long id;
        private String workerName;
        private String actionTitle;
        private String description;
        private String henCode;
        private String timestamp;
        private String eventType; // COMPLETED, SKIPPED, RESCHEDULED, ESCALATED
    }
}
