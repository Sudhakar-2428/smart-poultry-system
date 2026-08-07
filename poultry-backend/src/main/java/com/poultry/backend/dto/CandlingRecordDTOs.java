package com.poultry.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CandlingRecordDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandlingRecordRequest {
        @NotNull(message = "Incubator batch ID is required")
        private Long incubatorBatchId;

        @NotNull(message = "Candling date is required")
        private LocalDate candlingDate;

        @NotNull(message = "Candling day is required")
        @Min(value = 1, message = "Candling day must be at least 1")
        private Integer candlingDay;

        @NotNull(message = "Fertile eggs is required")
        @Min(value = 0, message = "Fertile eggs cannot be negative")
        private Integer fertileEggs;

        @NotNull(message = "Infertile eggs is required")
        @Min(value = 0, message = "Infertile eggs cannot be negative")
        private Integer infertileEggs;

        @NotNull(message = "Dead embryos is required")
        @Min(value = 0, message = "Dead embryos cannot be negative")
        private Integer deadEmbryos;

        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CandlingRecordResponse {
        private Long id;
        private Long incubatorBatchId;
        private String incubatorBatchCode;
        private LocalDate candlingDate;
        private Integer candlingDay;
        private Integer fertileEggs;
        private Integer infertileEggs;
        private Integer deadEmbryos;
        private String remarks;
        private LocalDateTime createdAt;
    }
}
