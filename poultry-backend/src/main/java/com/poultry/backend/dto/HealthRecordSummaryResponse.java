package com.poultry.backend.dto;

import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.HealthType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordSummaryResponse {
    private Long id;
    private String recordCode;
    private Long chickenId;
    private String chickenCode;
    private LocalDate recordDate;
    private HealthType healthType;
    private HealthStatus healthStatus;
    private String diseaseName;
    private String vaccinationName;
    private String veterinarian;
    private Boolean mortality;
}
