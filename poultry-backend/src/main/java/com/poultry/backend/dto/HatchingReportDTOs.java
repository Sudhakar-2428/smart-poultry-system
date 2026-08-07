package com.poultry.backend.dto;

import com.poultry.backend.entity.IncubationMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class HatchingReportDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HatchingReportResponse {
        private Long id;
        private String reportCode;
        private Long incubatorBatchId;
        private String hatchBatchCode;
        private String eggBatchCode;
        private String pairingCode;
        private LocalDateTime reportDate;
        private String farmName;
        private String generatedBy;

        // Mother Hen Details
        private String motherHenCode;
        private String motherHenName;
        private String motherHenBreed;
        private String motherHenAge;
        private String motherHenOrigin;

        // Father Rooster Details
        private String fatherRoosterCode;
        private String fatherRoosterName;
        private String fatherRoosterBreed;
        private String fatherRoosterAge;
        private String fatherRoosterOrigin;

        // Breeding Information
        private LocalDate pairingDate;
        private LocalDate eggLayingStartDate;
        private Integer collectionPeriodDays;
        private IncubationMethod incubationMethod;
        private String equipmentOrNest;

        // Egg Summary
        private Integer totalEggsCollected;
        private Integer eggsSelectedForHatching;
        private Integer healthyEggs;
        private Integer brokenEggs;
        private Integer rejectedEggs;

        // Candling Summary
        private Integer day7Fertile;
        private Integer day7Infertile;
        private Integer day7DeadEmbryos;

        private Integer day14Fertile;
        private Integer day14Infertile;
        private Integer day14DeadEmbryos;

        private Integer day18Fertile;
        private Integer day18Infertile;
        private Integer day18DeadEmbryos;

        // Hatch Results
        private Integer totalEggsSet;
        private Integer fertileEggs;
        private Integer hatchedChicks;
        private Integer healthyChicks;
        private Integer weakChicks;
        private Integer deadChicks;
        private Integer unhatchedEggs;
        private Double hatchSuccessPercentage;

        // Performance Analysis
        private Double fertilityRate;
        private Double hatchSuccessRate;
        private Double healthyChickRate;
        private Double lossPercentage;

        private LocalDateTime createdAt;
    }
}
