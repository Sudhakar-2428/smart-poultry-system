package com.poultry.backend.dto;

import com.poultry.backend.entity.PairStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class EggLayingDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggLayingDashboardStats {
        private Long totalLayingHens;
        private Long readyForEggCollection;
        private Long waitingPeriodHens;
        private Long activePairings;
        private Long archivedPairings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggLayingItemResponse {
        private Long pairId;
        private String pairCode;
        
        private Long femaleChickenId;
        private String femaleChickenCode;
        private String femaleChickenName;
        private String femaleChickenBreed;
        private String femaleChickenPhotoUrl;
        
        private Long maleChickenId;
        private String maleChickenCode;
        private String maleChickenName;
        private String maleChickenBreed;
        
        private LocalDate pairingDate;
        private Long daysSincePairing;
        private LocalDate expectedEggLayingDate;
        private String currentStage; // Waiting, Ready For Egg Collection, Transferred, Archived
        private PairStatus status;
        private Boolean isReadyForCollection;
        private Integer currentBatchNumber;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EggLayingHistoryResponse {
        private Long pairId;
        private String pairCode;
        
        private Long femaleChickenId;
        private String femaleChickenCode;
        private String femaleChickenName;
        
        private Long maleChickenId;
        private String maleChickenCode;
        private String maleChickenName;
        
        private LocalDate pairingDate;
        private LocalDateTime eggLayingStartedDate;
        private LocalDateTime transferDate;
        private Long durationDays;
        private Integer batchNumber;
        private String currentStatus;
    }
}
