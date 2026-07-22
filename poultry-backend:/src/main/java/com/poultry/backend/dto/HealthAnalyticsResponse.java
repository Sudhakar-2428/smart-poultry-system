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
public class HealthAnalyticsResponse {
    private Long totalVaccinations;
    private Long diseaseCount;
    private Long mortalityCount;
    private Long recoveryCount;
    private Long treatmentCount;
    private Double vaccinationCompliance;

    private Map<String, Long> diseaseDistributions;
    private Map<String, Long> vaccineComplianceByBatch;
}
