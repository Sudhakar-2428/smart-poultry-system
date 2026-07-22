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
public class HatchingReportResponse {
    private Long totalIncubated;
    private Long totalHatched;
    private Long totalFailed;
    private Double hatchRate;
    private Double averageIncubationDays;
    private Long activeIncubatorBatches;
    private Long completedIncubatorBatches;

    private Map<String, Double> hatchRateByBreed;
}
