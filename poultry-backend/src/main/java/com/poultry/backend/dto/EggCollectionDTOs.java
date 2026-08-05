package com.poultry.backend.dto;

import com.poultry.backend.entity.EggCollectionStatus;
import com.poultry.backend.entity.EggItemStatus;
import com.poultry.backend.entity.EggPurpose;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

public class EggCollectionDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggCollectionResponse {
        private Long id;
        private Long femaleChickenId;
        private String femaleChickenCode;
        private String femaleChickenName;
        private String femaleChickenBreed;
        private String femaleChickenPhotoUrl;
        
        private Long maleChickenId;
        private String maleChickenCode;
        private String maleChickenName;
        private String maleChickenBreed;
        
        private Long breedingPairId;
        private String pairCode;
        private LocalDate pairingDate;
        private Long daysSincePairing;
        private LocalDate eggLayingStartedDate;
        private Integer currentBatchNumber;
        private String batchCode; // e.g. EB-101-03
        
        private Integer todayEggCount;
        private Integer totalEggCount;
        private EggCollectionStatus status;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyEggRecordRequest {
        @NotNull(message = "Egg collection record ID is required")
        private Long eggCollectionId;

        @NotNull(message = "Collection date is required")
        private LocalDate collectionDate;

        @NotNull(message = "Number of eggs is required")
        @Min(value = 1, message = "Number of eggs must be at least 1")
        private Integer numberOfEggs;

        @NotNull(message = "Broken eggs count is required")
        @Min(value = 0, message = "Broken eggs count cannot be negative")
        private Integer brokenEggs;

        @Min(value = 0)
        private Integer damagedEggs;

        @Min(value = 0)
        private Integer healthyEggs;

        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggItemResponse {
        private Long id;
        private String eggCode;
        private Long femaleChickenId;
        private String femaleChickenCode;
        private String femaleChickenName;
        private Long maleChickenId;
        private String maleChickenCode;
        private String maleChickenName;
        private Integer batchNumber;
        private String batchCode; // e.g. EB-101-03
        private LocalDate collectionDate;
        private EggItemStatus status;
        private EggPurpose purpose;
        private Boolean isMovedToHatching;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateEggPurposeRequest {
        @NotNull(message = "Egg ID is required")
        private Long eggId;

        @NotNull(message = "Purpose is required")
        private EggPurpose purpose;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendToHatchingRequest {
        @NotNull(message = "List of egg IDs is required")
        private List<Long> eggIds;

        private String targetBatchCode;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardStatsResponse {
        private Long totalActiveLayingHens;
        private Long todayEggs;
        private Long weeklyEggs;
        private Long monthlyEggs;
        private Long eggsForHatching;
        private Long eggsForSale;
        private Long eggsForHomeUse;
        private Long brokenEggs;
        private Double averageEggsPerHen;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchSummaryResponse {
        private Integer batchNumber;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer totalEggs;
        private Integer healthyEggs;
        private Integer brokenEggs;
        private Integer selectedForHatching;
        private Integer selectedForSale;
        private Integer selectedForHomeUse;
        private String batchStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HenLayingProfileResponse {
        private Long henId;
        private String henCode;
        private String henName;
        private String henBreed;
        private String age;
        private Double weight;
        private String healthStatus;
        
        private Long roosterId;
        private String roosterCode;
        private String roosterName;
        
        private LocalDate pairingDate;
        private LocalDate eggLayingStartedDate;
        private Integer currentBatchNumber;
        private Integer totalEggs;
        
        private List<BatchSummaryResponse> batchHistory;
    }
}
