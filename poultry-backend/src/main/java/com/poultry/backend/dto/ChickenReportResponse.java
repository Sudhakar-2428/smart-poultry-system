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
public class ChickenReportResponse {
    private Long totalChickens;
    private Long activeCount;
    private Long deadCount;
    private Long soldCount;
    private Double averageWeight;
    private Double averageAgeDays;
    private Double mortalityRate;
    
    private Map<String, Long> breedDistribution;
    private Map<String, Long> genderDistribution;
    private Map<String, Long> categoryDistribution;
    private Map<String, Long> statusDistribution;
}
