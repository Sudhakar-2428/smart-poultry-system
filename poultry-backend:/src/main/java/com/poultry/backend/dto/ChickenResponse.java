package com.poultry.backend.dto;

import com.poultry.backend.entity.Breed;
import com.poultry.backend.entity.ChickenCategory;
import com.poultry.backend.entity.ChickenStatus;
import com.poultry.backend.entity.Gender;
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
    private Long motherId;
    private Long fatherId;
    private Long pairId;
    private String photoUrl;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
