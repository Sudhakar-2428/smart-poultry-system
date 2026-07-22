package com.poultry.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrowthReportResponse {
    private Double averageWeightGain;
    private Double averageWeight;
    private Double averageHeight;
    private Long growthRecordsCount;

    private Map<String, Long> growthStageDistribution;
    private Map<String, Long> healthStatusDistribution;
}
