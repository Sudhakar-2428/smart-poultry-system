package com.poultry.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ChickenAdvancedDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PairingActionRequest {
        @NotNull(message = "Rooster ID is required")
        private Long maleChickenId;

        @NotNull(message = "Hen ID is required")
        private Long femaleChickenId;

        private LocalDate startDate;
        private String purpose;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HatchBatchActionRequest {
        private String batchCode;
        private Long roosterId;
        private Long henId;
        private LocalDate eggCollectionDate;
        private Integer totalEggs;
        private Integer eggsSelected;
        private LocalDate expectedHatchDate;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HatchResultActionRequest {
        private Long incubatorBatchId;
        private Integer totalEggs;
        private Integer fertileEggs;
        private Integer hatchedChicks;
        private Integer failedEggs;
        private Integer maleChicks;
        private Integer femaleChicks;
        private Integer deadChicks;
        private Integer healthyChicks;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BroodingActionRequest {
        private Long chickenId;
        private String brooderHouse;
        private String pen;
        private LocalDate transferDate;
        private String worker;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeathRecordRequest {
        @NotNull(message = "Death date is required")
        private LocalDate deathDate;
        private String causeOfDeath;
        private String diseaseName;
        private String postmortemNotes;
        private String worker;
        private String photoUrl;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ExpenseActionRequest {
        @NotBlank(message = "Expense type is required")
        private String expenseType;

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be positive")
        private Double amount;

        private LocalDate transactionDate;
        private String invoiceNumber;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FeedRecordActionRequest {
        @NotBlank(message = "Feed type is required")
        private String feedType;

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private Double quantityKg;

        private Double cost;
        private String supplier;
        private LocalDate recordDate;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PhotoCaptureRequest {
        @NotBlank(message = "Photo payload is required")
        private String photoUrl;
        private String caption;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkerAssignmentRequest {
        @NotNull(message = "Worker is required")
        private String workerName;
        private LocalDate assignmentDate;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReminderActionRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Reminder type is required")
        private String reminderType;

        @NotNull(message = "Due date is required")
        private LocalDate dueDate;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChickenNoteRequest {
        @NotBlank(message = "Note content is required")
        private String noteText;
        private String author;
        private String tags;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AIHealthAnalysisResponse {
        private String diseaseRiskLevel;
        private Double diseaseRiskScore;
        private String weightStatus;
        private String eggProductionForecast;
        private String hatchRateForecast;
        private List<String> recommendations;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BreedingPerformanceResponse {
        private Integer totalPairings;
        private Double fertilityRate;
        private Double averageHatchRate;
        private Integer totalChicksProduced;
        private String performanceGrade;
        private List<String> batchSummaryList;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketValueResponse {
        private Double estimatedMarketValue;
        private Double basePricePerKg;
        private Double healthMultiplier;
        private Double breedMultiplier;
        private String valuationGrade;
        private String breakdownSummary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RelatedChickensResponse {
        private ChickenSummaryResponse father;
        private ChickenSummaryResponse mother;
        private List<ChickenSummaryResponse> offspring;
        private List<ChickenSummaryResponse> siblings;
        private String currentPairCode;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActivityItemDTO {
        private String timestamp;
        private String user;
        private String actionType;
        private String description;
        private String ipAddress;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditItemDTO {
        private String timestamp;
        private String fieldName;
        private String oldValue;
        private String newValue;
        private String modifiedBy;
    }
}
