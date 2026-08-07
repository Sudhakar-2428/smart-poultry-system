package com.poultry.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class ChickenTimelineDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEventDTO {
        private Long id;
        private Long chickenId;
        private String chickenCode;
        private String chickenName;
        private String eventType;
        private String title;
        private String description;
        private String createdBy;
        private String moduleName;
        private Long relatedEntityId;
        private String moduleNavigationLink;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTimelineNoteRequest {
        @NotBlank(message = "Note title is required")
        private String title;

        @NotBlank(message = "Note description is required")
        private String description;

        private String moduleName;
        private Long relatedEntityId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineReportDTO {
        private String reportTitle;
        private Long totalEvents;
        private List<TimelineEventDTO> events;
    }
}
