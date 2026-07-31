package com.poultry.backend.dto;

import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
import com.poultry.backend.entity.ChickenOrigin;
import com.poultry.backend.entity.HealthStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenSummaryResponse {
    private Long id;
    private String chickenCode;
    private String name;
    private Breed breed;
    private ChickenCategory category;
    private Gender gender;
    private LocalDate dateOfBirth;
    private Long ageInDays;
    private Long ageInMonths;
    private Double weight;
    private ChickenStatus status;
    private HealthStatus healthStatus;
    private ChickenOrigin origin;
    private String photoUrl;
    private String legBandNumber;
    private String wingTagNumber;
    private String remarks;
}

