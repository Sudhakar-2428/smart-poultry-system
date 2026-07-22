package com.poultry.backend.dto;

import com.poultry.backend.entity.BreedingPurpose;
import com.poultry.backend.entity.PairStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BreedingPairSummaryResponse {
    private Long id;
    private String pairCode;
    private Long maleChickenId;
    private String maleChickenCode;
    private Long femaleChickenId;
    private String femaleChickenCode;
    private PairStatus status;
    private BreedingPurpose breedingPurpose;
    private LocalDate startDate;
    private LocalDate endDate;
}
