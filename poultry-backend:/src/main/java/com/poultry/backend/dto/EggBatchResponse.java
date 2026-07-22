package com.poultry.backend.dto;

import com.poultry.backend.entity.EggBatchStatus;
import com.poultry.backend.entity.EggPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggBatchResponse {
    private Long id;
    private String batchCode;
    private LocalDate batchDate;
    private Long sourceHenId;
    private String sourceHenCode;
    private Integer totalEggs;
    private Integer goodEggs;
    private Integer damagedEggs;
    private EggBatchStatus status;
    private EggPurpose purpose;
    private LocalDate expectedHatchDate;
    private LocalDate actualHatchDate;
    private Long daysRemaining;
    private Long batchAge;
    private Double hatchPercentage;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
