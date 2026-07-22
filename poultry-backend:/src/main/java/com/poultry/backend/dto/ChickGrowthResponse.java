package com.poultry.backend.dto;

import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.GrowthStage;
import com.poultry.backend.entity.HealthStatus;
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
public class ChickGrowthResponse {
    private Long id;
    private Long chickenId;
    private String chickenCode;
    private LocalDate growthDate;
    private Integer ageInDays;
    private Double weight;
    private Double height;
    private HealthStatus healthStatus;
    private GrowthStage growthStage;
    private Gender gender;
    private String remarks;

    // Dynamically calculated
    private Integer currentAge;
    private Double growthProgressPct;
    private Integer daysUntilAdultTransition;
    private GrowthStage currentGrowthStage;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
