package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HatchResultResponse {
    private Long id;
    private Long incubatorBatchId;
    private String incubatorBatchCode;
    private Integer totalEggs;
    private Integer fertileEggs;
    private Integer hatchedChicks;
    private Integer healthyChicks;
    private Integer weakChicks;
    private Integer deadChicks;
    private Integer deadEmbryos;
    private Integer unhatchedEggs;
    private Double hatchPercentage;
    private LocalDate recordedDate;
    private String remarks;
}
