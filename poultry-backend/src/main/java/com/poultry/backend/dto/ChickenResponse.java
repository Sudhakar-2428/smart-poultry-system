package com.poultry.backend.dto;

import com.poultry.backend.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChickenResponse {
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
    private String color;
    private ChickenStatus status;
    private HealthStatus healthStatus;
    private ChickenOrigin origin;
    private LocalDate purchaseDate;
    private Double purchaseCost;
    private String supplierName;
    private String supplierContact;
    private String wingTagNumber;
    private String legBandNumber;
    private Boolean vaccinated;
    private List<ChickenVaccinationDTO> vaccinations;
    private Long motherId;
    private String motherCode;
    private Long fatherId;
    private String fatherCode;
    private Long pairId;
    private String photoUrl;
    private String remarks;
    private Integer vaccinationCount;
    private List<ChickenTimelineEventDTO> timeline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
