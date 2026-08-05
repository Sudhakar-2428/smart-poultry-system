package com.poultry.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ChickenActionDTOs {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeightUpdateRequest {
        @NotNull(message = "Weight is required")
        @Positive(message = "Weight must be positive")
        private Double weight;

        private LocalDate measuredDate;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransferRequest {
        @NotBlank(message = "Transfer farm or location is required")
        private String transferFarm;

        private String transferShed;
        private String transferReason;
        private LocalDate transferDate;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellRequest {
        @NotBlank(message = "Buyer name is required")
        private String buyerName;

        private String buyerContact;

        @NotNull(message = "Sale price is required")
        @Positive(message = "Sale price must be positive")
        private Double salePrice;

        private LocalDate saleDate;
        private String paymentMethod;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ChickenFullProfileReportDTO {
        private FarmInfoDTO farmInfo;
        private ChickenProfileDTO chickenProfile;
        private List<HealthRecordItemDTO> healthHistory;
        private List<WeightRecordItemDTO> weightHistory;
        private FinancialSummaryDTO financialInfo;
        private HenBreedingReportDTO henBreedingReport;
        private RoosterBreedingReportDTO roosterBreedingReport;
        private List<ChickenTimelineEventDTO> timeline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FarmInfoDTO {
        private String farmName;
        private String ownerName;
        private String address;
        private String phone;
        private String generatedDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChickenProfileDTO {
        private Long id;
        private String chickenCode;
        private String name;
        private String category;
        private String breed;
        private String gender;
        private String origin;
        private String healthStatus;
        private Double currentWeight;
        private LocalDate registrationDate;
        private LocalDate dateOfBirth;
        private String ageText;
        private String status;
        private String qrCodeString;
        private String photoUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HealthRecordItemDTO {
        private Long id;
        private LocalDate recordDate;
        private String healthType;
        private String diseaseName;
        private String medicineName;
        private String vaccinationName;
        private String treatmentDetails;
        private String veterinarian;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WeightRecordItemDTO {
        private LocalDate date;
        private Double weight;
        private String growthTrend;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FinancialSummaryDTO {
        private Double currentValue;
        private Double purchaseCost;
        private Double sellingPrice;
        private Double profit;
        private Double totalExpenses;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HenBreedingReportDTO {
        private List<HatchBatchDetailDTO> hatchBatches;
        private LifetimeStatsDTO lifetimeStats;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HatchBatchDetailDTO {
        private String batchNumber;
        private String roosterId;
        private String roosterName;
        private LocalDate pairingDate;
        private LocalDate eggCollectionDate;
        private Integer totalEggsCollected;
        private Integer eggsSelectedForHatching;
        private Integer eggsFertile;
        private Integer eggsHatched;
        private Integer eggsFailed;
        private Integer chicksBorn;
        private Integer maleChicks;
        private Integer femaleChicks;
        private Integer mortality;
        private Double batchSuccessPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LifetimeStatsDTO {
        private Integer totalEggsLaid;
        private Integer totalHatchBatches;
        private Integer totalChicksBorn;
        private Integer totalFertileEggs;
        private Double totalHatchSuccessPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoosterBreedingReportDTO {
        private List<RoosterHenPairingDTO> pairedHens;
        private RoosterSummaryDTO summary;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoosterHenPairingDTO {
        private String henId;
        private String henName;
        private LocalDate pairingDate;
        private String batchNumber;
        private Integer eggsProduced;
        private Integer eggsFertile;
        private Integer eggsHatched;
        private Integer chicksBorn;
        private Double batchSuccessPercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RoosterSummaryDTO {
        private Integer totalHensPaired;
        private Integer totalHatchBatches;
        private Integer totalFertileEggs;
        private Integer totalChicksProduced;
        private Double averageHatchSuccessPercentage;
    }
}
