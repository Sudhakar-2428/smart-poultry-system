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
public class BreedingReportResponse {
    private Long totalBreedingPairs;
    private Long activeBreedingPairs;
    private Long totalExpectedEggProduction;

    private Map<String, Long> breedingPurposeDistribution;
    private Map<String, Long> statusDistribution;
}
