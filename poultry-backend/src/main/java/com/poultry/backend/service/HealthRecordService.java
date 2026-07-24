package com.poultry.backend.service;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.HealthStatus;
import com.poultry.backend.entity.HealthType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface HealthRecordService {

    HealthRecordResponse createHealthRecord(HealthRecordRequest request);

    HealthRecordResponse getHealthRecordById(Long id);

    HealthRecordResponse updateHealthRecord(Long id, HealthRecordRequest request);

    void deleteHealthRecord(Long id);

    Page<HealthRecordSummaryResponse> searchHealthRecords(
            Long chickenId,
            HealthType healthType,
            HealthStatus healthStatus,
            String diseaseName,
            String vaccinationName,
            String veterinarian,
            LocalDate startDate,
            LocalDate endDate,
            Boolean mortality,
            Pageable pageable
    );

    // Reporting support methods
    long getTotalVaccinations();

    long getDiseaseCount();

    long getMortalityCount();

    long getRecoveryCount();

    long getTreatmentCount();

    double getVaccinationCompliance();
}
