package com.poultry.backend.dto;

import com.poultry.backend.entity.BreedingPurpose;
import com.poultry.backend.entity.PairStatus;
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
public class BreedingPairResponse {
    private Long id;
    private String pairCode;
    private Long maleChickenId;
    private String maleChickenCode;
    private Long femaleChickenId;
    private String femaleChickenCode;
    private LocalDate startDate;
    private LocalDate endDate;
    private PairStatus status;
    private BreedingPurpose breedingPurpose;
    private Integer expectedEggProduction;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
