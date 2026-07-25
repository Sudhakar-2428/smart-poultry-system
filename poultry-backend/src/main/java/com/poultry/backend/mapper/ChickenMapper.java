package com.poultry.backend.mapper;

import com.poultry.backend.dto.*;
import com.poultry.backend.entity.*;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChickenMapper {

    /**
     * Map Chicken entity to ChickenResponse.
     */
    public ChickenResponse toResponse(Chicken chicken) {
        if (chicken == null) {
            return null;
        }

        LocalDate dob = chicken.getDateOfBirth();
        long ageInDays = dob != null ? ChronoUnit.DAYS.between(dob, LocalDate.now()) : 0;
        long ageInMonths = dob != null ? ChronoUnit.MONTHS.between(dob, LocalDate.now()) : 0;

        List<ChickenVaccinationDTO> vDtos = chicken.getVaccinations() != null ? chicken.getVaccinations().stream()
                .map(v -> ChickenVaccinationDTO.builder()
                        .vaccineName(v.getVaccineName())
                        .vaccinationDate(v.getVaccinationDate())
                        .nextDueDate(v.getNextDueDate())
                        .notes(v.getNotes())
                        .build())
                .collect(Collectors.toList()) : new ArrayList<>();

        return ChickenResponse.builder()
                .id(chicken.getId())
                .chickenCode(chicken.getChickenCode())
                .name(chicken.getName())
                .breed(chicken.getBreed())
                .category(chicken.getCategory())
                .gender(chicken.getGender())
                .dateOfBirth(dob)
                .ageInDays(Math.max(0, ageInDays))
                .ageInMonths(Math.max(0, ageInMonths))
                .weight(chicken.getWeight())
                .color(chicken.getColor())
                .status(chicken.getStatus())
                .healthStatus(chicken.getHealthStatus())
                .origin(chicken.getOrigin())
                .purchaseDate(chicken.getPurchaseDate())
                .purchaseCost(chicken.getPurchaseCost())
                .supplierName(chicken.getSupplierName())
                .supplierContact(chicken.getSupplierContact())
                .wingTagNumber(chicken.getWingTagNumber())
                .legBandNumber(chicken.getLegBandNumber())
                .vaccinated(chicken.getVaccinated())
                .vaccinations(vDtos)
                .motherId(chicken.getMotherId())
                .fatherId(chicken.getFatherId())
                .pairId(chicken.getPairId())
                .photoUrl(chicken.getPhotoUrl())
                .remarks(chicken.getRemarks())
                .createdAt(chicken.getCreatedAt())
                .updatedAt(chicken.getUpdatedAt())
                .build();
    }

    /**
     * Map Chicken entity to ChickenSummaryResponse.
     */
    public ChickenSummaryResponse toSummaryResponse(Chicken chicken) {
        if (chicken == null) {
            return null;
        }

        LocalDate dob = chicken.getDateOfBirth();
        long ageInDays = dob != null ? ChronoUnit.DAYS.between(dob, LocalDate.now()) : 0;
        long ageInMonths = dob != null ? ChronoUnit.MONTHS.between(dob, LocalDate.now()) : 0;

        return ChickenSummaryResponse.builder()
                .id(chicken.getId())
                .chickenCode(chicken.getChickenCode())
                .name(chicken.getName())
                .breed(chicken.getBreed())
                .category(chicken.getCategory())
                .gender(chicken.getGender())
                .dateOfBirth(dob)
                .ageInDays(Math.max(0, ageInDays))
                .ageInMonths(Math.max(0, ageInMonths))
                .weight(chicken.getWeight())
                .status(chicken.getStatus())
                .build();
    }

    /**
     * Map ChickenRequest to Chicken entity.
     */
    public Chicken toEntity(ChickenRequest request) {
        if (request == null) {
            return null;
        }

        List<ChickenVaccination> vaccinations = request.getVaccinations() != null ? request.getVaccinations().stream()
                .map(dto -> ChickenVaccination.builder()
                        .vaccineName(dto.getVaccineName())
                        .vaccinationDate(dto.getVaccinationDate())
                        .nextDueDate(dto.getNextDueDate())
                        .notes(dto.getNotes())
                        .build())
                .collect(Collectors.toList()) : new ArrayList<>();

        return Chicken.builder()
                .chickenCode(request.getChickenCode())
                .name(request.getName())
                .breed(request.getBreed())
                .category(request.getCategory())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .weight(request.getWeight())
                .color(request.getColor())
                .status(request.getStatus())
                .healthStatus(request.getHealthStatus() != null ? request.getHealthStatus() : HealthStatus.HEALTHY)
                .origin(request.getOrigin() != null ? request.getOrigin() : ChickenOrigin.FARM_BORN)
                .purchaseDate(request.getPurchaseDate())
                .purchaseCost(request.getPurchaseCost())
                .supplierName(request.getSupplierName())
                .supplierContact(request.getSupplierContact())
                .wingTagNumber(request.getWingTagNumber())
                .legBandNumber(request.getLegBandNumber())
                .vaccinated(request.getVaccinated() != null ? request.getVaccinated() : false)
                .vaccinations(vaccinations)
                .motherId(request.getMotherId())
                .fatherId(request.getFatherId())
                .pairId(request.getPairId())
                .photoUrl(request.getPhotoUrl())
                .remarks(request.getRemarks())
                .build();
    }

    /**
     * Map ChickenRequest updates onto an existing Chicken entity.
     */
    public void updateEntityFromRequest(ChickenRequest request, Chicken chicken) {
        if (request == null || chicken == null) {
            return;
        }

        if (request.getChickenCode() != null && !request.getChickenCode().isBlank()) {
            chicken.setChickenCode(request.getChickenCode());
        }
        chicken.setName(request.getName());
        chicken.setBreed(request.getBreed());
        chicken.setCategory(request.getCategory());
        chicken.setGender(request.getGender());
        chicken.setDateOfBirth(request.getDateOfBirth());
        chicken.setWeight(request.getWeight());
        chicken.setColor(request.getColor());
        chicken.setStatus(request.getStatus());
        if (request.getHealthStatus() != null) chicken.setHealthStatus(request.getHealthStatus());
        if (request.getOrigin() != null) chicken.setOrigin(request.getOrigin());
        chicken.setPurchaseDate(request.getPurchaseDate());
        chicken.setPurchaseCost(request.getPurchaseCost());
        chicken.setSupplierName(request.getSupplierName());
        chicken.setSupplierContact(request.getSupplierContact());
        chicken.setWingTagNumber(request.getWingTagNumber());
        chicken.setLegBandNumber(request.getLegBandNumber());
        if (request.getVaccinated() != null) chicken.setVaccinated(request.getVaccinated());

        if (request.getVaccinations() != null) {
            List<ChickenVaccination> vaccinations = request.getVaccinations().stream()
                    .map(dto -> ChickenVaccination.builder()
                            .vaccineName(dto.getVaccineName())
                            .vaccinationDate(dto.getVaccinationDate())
                            .nextDueDate(dto.getNextDueDate())
                            .notes(dto.getNotes())
                            .build())
                    .collect(Collectors.toList());
            chicken.setVaccinations(vaccinations);
        }

        chicken.setMotherId(request.getMotherId());
        chicken.setFatherId(request.getFatherId());
        chicken.setPairId(request.getPairId());
        if (request.getPhotoUrl() != null) chicken.setPhotoUrl(request.getPhotoUrl());
        chicken.setRemarks(request.getRemarks());
    }
}
