package com.poultry.backend.mapper;

import com.poultry.backend.dto.HealthRecordRequest;
import com.poultry.backend.dto.HealthRecordResponse;
import com.poultry.backend.dto.HealthRecordSummaryResponse;
import com.poultry.backend.entity.HealthRecord;
import org.springframework.stereotype.Component;

@Component
public class HealthRecordMapper {

    public HealthRecord toEntity(HealthRecordRequest request) {
        if (request == null) {
            return null;
        }
        return HealthRecord.builder()
                .recordCode(request.getRecordCode())
                .recordDate(request.getRecordDate())
                .healthType(request.getHealthType())
                .diseaseName(request.getDiseaseName())
                .symptoms(request.getSymptoms())
                .diagnosis(request.getDiagnosis())
                .treatment(request.getTreatment())
                .medicineName(request.getMedicineName())
                .medicineDose(request.getMedicineDose())
                .vaccinationName(request.getVaccinationName())
                .vaccinationBatch(request.getVaccinationBatch())
                .nextVaccinationDate(request.getNextVaccinationDate())
                .veterinarian(request.getVeterinarian())
                .healthStatus(request.getHealthStatus())
                .mortality(request.getMortality() != null ? request.getMortality() : false)
                .remarks(request.getRemarks())
                .build();
    }

    public HealthRecordResponse toResponse(HealthRecord record) {
        if (record == null) {
            return null;
        }
        return HealthRecordResponse.builder()
                .id(record.getId())
                .recordCode(record.getRecordCode())
                .chickenId(record.getChicken() != null ? record.getChicken().getId() : null)
                .chickenCode(record.getChicken() != null ? record.getChicken().getChickenCode() : "")
                .recordDate(record.getRecordDate())
                .healthType(record.getHealthType())
                .diseaseName(record.getDiseaseName())
                .symptoms(record.getSymptoms())
                .diagnosis(record.getDiagnosis())
                .treatment(record.getTreatment())
                .medicineName(record.getMedicineName())
                .medicineDose(record.getMedicineDose())
                .vaccinationName(record.getVaccinationName())
                .vaccinationBatch(record.getVaccinationBatch())
                .nextVaccinationDate(record.getNextVaccinationDate())
                .veterinarian(record.getVeterinarian())
                .healthStatus(record.getHealthStatus())
                .mortality(record.getMortality())
                .remarks(record.getRemarks())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    public HealthRecordSummaryResponse toSummaryResponse(HealthRecord record) {
        if (record == null) {
            return null;
        }
        return HealthRecordSummaryResponse.builder()
                .id(record.getId())
                .recordCode(record.getRecordCode())
                .chickenId(record.getChicken() != null ? record.getChicken().getId() : null)
                .chickenCode(record.getChicken() != null ? record.getChicken().getChickenCode() : "")
                .recordDate(record.getRecordDate())
                .healthType(record.getHealthType())
                .healthStatus(record.getHealthStatus())
                .diseaseName(record.getDiseaseName())
                .vaccinationName(record.getVaccinationName())
                .veterinarian(record.getVeterinarian())
                .mortality(record.getMortality())
                .build();
    }
}
