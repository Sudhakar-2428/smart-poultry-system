package com.poultry.backend.dto;

import com.poultry.backend.entity.BreedingPurpose;
import com.poultry.backend.entity.PairStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class BreedingPairRequest {

    @NotNull(message = "Pair code is required")
    @Size(min = 2, max = 50, message = "Pair code length must be between 2 and 50 characters")
    private String pairCode;

    @NotNull(message = "Male chicken ID is required")
    private Long maleChickenId;

    @NotNull(message = "Female chicken ID is required")
    private Long femaleChickenId;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;

    @NotNull(message = "Pair status is required")
    private PairStatus status;

    @NotNull(message = "Breeding purpose is required")
    private BreedingPurpose breedingPurpose;

    @PositiveOrZero(message = "Expected egg production must be positive or zero")
    private Integer expectedEggProduction;

    private String remarks;
}
