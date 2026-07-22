package com.poultry.backend.dto;

import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.HealthType;
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
public class HealthRecordResponse {
    private Long id;
    private String recordCode;
    private Long chickenId;
    private String chickenCode;
    private LocalDate recordDate;
    private HealthType healthType;
    private String diseaseName;
    private String symptoms;
    private String diagnosis;
    private String treatment;
    private String medicineName;
    private String medicineDose;
    private String vaccinationName;
    private String vaccinationBatch;
    private LocalDate nextVaccinationDate;
    private String veterinarian;
    private HealthStatus healthStatus;
    private Boolean mortality;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
