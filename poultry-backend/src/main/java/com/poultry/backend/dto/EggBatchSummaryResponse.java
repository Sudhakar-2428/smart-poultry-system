package com.poultry.backend.dto;

import com.poultry.backend.entity.EggBatchStatus;
import com.poultry.backend.entity.EggPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EggBatchSummaryResponse {
    private Long id;
    private String batchCode;
    private LocalDate batchDate;
    private Integer totalEggs;
    private Integer goodEggs;
    private Integer damagedEggs;
    private EggBatchStatus status;
    private EggPurpose purpose;
    private LocalDate expectedHatchDate;
    private Long daysRemaining;
    private Long batchAge;
}
