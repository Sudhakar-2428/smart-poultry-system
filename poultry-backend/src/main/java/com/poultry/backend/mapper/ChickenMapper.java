package com.poultry.backend.mapper;

import com.poultry.backend.dto.ChickenRequest;
import com.poultry.backend.dto.ChickenResponse;
import com.poultry.backend.dto.ChickenSummaryResponse;
import com.poultry.backend.entity.Chicken;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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

        chicken.setChickenCode(request.getChickenCode());
        chicken.setName(request.getName());
        chicken.setBreed(request.getBreed());
        chicken.setCategory(request.getCategory());
        chicken.setGender(request.getGender());
        chicken.setDateOfBirth(request.getDateOfBirth());
        chicken.setWeight(request.getWeight());
        chicken.setColor(request.getColor());
        chicken.setStatus(request.getStatus());
        chicken.setMotherId(request.getMotherId());
        chicken.setFatherId(request.getFatherId());
        chicken.setPairId(request.getPairId());
        chicken.setPhotoUrl(request.getPhotoUrl());
        chicken.setRemarks(request.getRemarks());
    }
}
