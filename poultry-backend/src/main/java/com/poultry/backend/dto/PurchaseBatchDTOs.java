package com.poultry.backend.dto;

import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.Gender;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PurchaseBatchDTOs {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreatePurchaseBatchRequest {
        private String batchCode; // Optional, auto-generated if null (PB01, PB02)

        @NotNull(message = "Supplier name is required")
        private String supplierName;

        private String supplierContact;

        @NotNull(message = "Purchase date is required")
        private LocalDate purchaseDate;

        private String invoiceNumber;

        private BigDecimal purchaseCost;

        private BigDecimal transportCost;

        @NotNull(message = "Total chickens count is required")
        @Min(value = 1, message = "Total chickens count must be at least 1")
        private Integer totalChickensCount;

        private Breed breed;
        private ChickenCategory category;
        private Gender gender;
        private Double averageWeight;
        private String remarks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchasedChickenDTO {
        private Long id;
        private String chickenCode;
        private String name;
        private String breed;
        private String category;
        private String gender;
        private String origin;
        private LocalDate purchaseDate;
        private Double purchaseCost;
        private String supplierName;
        private String supplierContact;
        private String purchaseBatchCode;
        private String qrCodeUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseBatchResponse {
        private Long id;
        private String batchCode;
        private String supplierName;
        private String supplierContact;
        private LocalDate purchaseDate;
        private String invoiceNumber;
        private BigDecimal purchaseCost;
        private BigDecimal transportCost;
        private Integer totalChickensCount;
        private String remarks;
        private List<PurchasedChickenDTO> registeredChickens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchasedChickenReportDTO {
        private String reportTitle;
        private Long totalBatches;
        private Long totalChickens;
        private BigDecimal totalSpend;
        private List<PurchasedChickenDTO> chickens;
    }
}
