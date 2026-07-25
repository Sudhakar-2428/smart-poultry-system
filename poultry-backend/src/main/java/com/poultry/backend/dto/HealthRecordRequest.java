package com.poultry.backend.dto;

import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.HealthType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthRecordRequest {

    @NotNull(message = "Record code is required")
    @Size(min = 2, max = 50, message = "Record code must be between 2 and 50 characters")
    private String recordCode;

    @NotNull(message = "Chicken ID is required")
    private Long chickenId;

    @NotNull(message = "Record date is required")
    private LocalDate recordDate;

    @NotNull(message = "Health type is required")
    private HealthType healthType;

    @Size(max = 100, message = "Disease name must be at most 100 characters")
    private String diseaseName;

    private String symptoms;

    private String diagnosis;

    private String treatment;

    @Size(max = 100, message = "Medicine name must be at most 100 characters")
    private String medicineName;

    @Size(max = 100, message = "Medicine dose must be at most 100 characters")
    private String medicineDose;

    @Size(max = 100, message = "Vaccination name must be at most 100 characters")
    private String vaccinationName;

    @Size(max = 50, message = "Vaccination batch must be at most 50 characters")
    private String vaccinationBatch;

    private String manufacturer;

    private String administeredBy;

    private LocalDate followUpDate;

    private LocalDate nextVaccinationDate;

    @NotNull(message = "Veterinarian is required")
    @Size(min = 2, max = 100, message = "Veterinarian must be between 2 and 100 characters")
    private String veterinarian;

    @NotNull(message = "Health status is required")
    private HealthStatus healthStatus;

    private Boolean mortality;

    private String remarks;
}
