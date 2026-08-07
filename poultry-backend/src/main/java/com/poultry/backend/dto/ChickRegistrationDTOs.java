package com.poultry.backend.dto;

import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

public class ChickRegistrationDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisteredChickDTO {
        private Long id;
        private String chickenCode;
        private Long motherId;
        private String motherCode;
        private String motherName;
        private Long fatherId;
        private String fatherCode;
        private String fatherName;
        private Breed breed;
        private ChickenCategory category;
        private Gender gender;
        private String origin;
        private LocalDate dateOfBirth;
        private HealthStatus healthStatus;
        private ChickenStatus status;
        private String hatchBatchCode;
        private String eggBatchCode;
        private String pairingCode;
        private String qrCodeUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChickRegistrationSummaryResponse {
        private String hatchBatchCode;
        private Integer totalRegisteredChicks;
        private String motherHenCode;
        private String fatherRoosterCode;
        private Integer hatchBatchSequence;
        private List<RegisteredChickDTO> registeredChicks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParentChickStatsResponse {
        private Long chickenId;
        private String chickenCode;
        private String name;
        private String gender;
        private Long totalHatchBatches;
        private Long totalChicksProduced;
        private Long currentHatchChicks;
        private Long partnerHensCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChickReportDTO {
        private String reportTitle;
        private String groupName;
        private Long totalChicks;
        private Double healthyPercentage;
        private List<RegisteredChickDTO> chicks;
    }
}
